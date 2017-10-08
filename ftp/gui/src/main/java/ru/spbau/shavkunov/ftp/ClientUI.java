package ru.spbau.shavkunov.ftp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.FileNotExistsException;
import ru.spbau.shavkunov.ftp.exceptions.NotConnectedException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static ru.spbau.shavkunov.ftp.NetworkConstants.PORT;
import static ru.spbau.shavkunov.ftp.NetworkConstants.standardDownloadsFolder;

/**
 * Graphical User Interface for client.
 */
public class ClientUI extends Application {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(ClientUI.class);

    /**
     * Server root directory.
     */
    private static @NotNull Path rootPath = Paths.get("test");

    /**
     * File client for downloading files from server.
     */
    private @Nullable Client client;

    /**
     * Directory, which is current listed in UI.
     */
    private @NotNull volatile Path currentDirectory = rootPath;

    /**
     * Response of the server. There are list of files located in currentDirectory.
     * Map of filename is true, if filename is directory on server side, false in other case.
     */
    private @NotNull Map<String, Boolean> filesInDirectory;

    /**
     * Current list view for visualize filesInDirectory.
     */
    private @Nullable ListView listView;

    /**
     * Directory, where downloaded files are stored.
     */
    private @NotNull Path downloads = standardDownloadsFolder;

    /**
     * Changeable items for setting listView content.
     */
    private @NotNull ObservableList<String> items = FXCollections.observableArrayList();

    /**
     * Label for showing download folder.
     */
    private @Nullable Label labelSelectedDirectory;

    /**
     * Button for coming back from folder.
     */
    private @Nullable Button backButton;

    @Override
    public void start(@NotNull Stage stage) throws Exception {
        // starting the server.
        Server server = new FileServer(PORT);
        server.start();

        // init scene with group objects
        stage.setTitle("Ftp server");
        Scene startScene = new Scene(new Group());

        Image[] createdImages = initImages();

        initSceneObjects(startScene, createdImages, stage);

        stage.setScene(startScene);
        stage.show();
    }

    /**
     * Init images of files and folders
     * @return array of two elements. First is image for regular file. Second is image for folder.
     */
    private Image[] initImages() {
        // searching for image files
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("file.png").getFile());
        File folder = new File(classLoader.getResource("folder.png").getFile());

        // creating image files
        Image fileImage = new Image(file.toURI().toString(), 32, 32, true, true, true);
        Image folderImage = new Image(folder.toURI().toString(), 32, 32, true, true, true);
        Image[] images = {fileImage, folderImage};

        return images;
    }

    /**
     * Updating listView for appropriate content.
     * @param images images for files and folder.
     */
    private void updateListView(@NotNull Image[] images) {
        Optional<Map<String, Boolean>> result = null;
        try {
            result = client.executeList(currentDirectory.toFile().toString());
        } catch (FileNotExistsException e) {
            // should not happen from GUI.
            e.printStackTrace();
        }

        if (!result.isPresent()) {
            // network closed
            items.clear();
            return;
        }

        filesInDirectory = result.get();

        if (filesInDirectory.size() > 0) {
            // cleaning previous content to add a new one
            items.clear();
        } else {
            // don't need to show a regular file
            currentDirectory = currentDirectory.getParent();
        }

        creatingCustomCells(images);

        // This changes are making out of JavaFX thread. Running later for avoiding null pointers.
        for (String filename : filesInDirectory.keySet()) {
            Platform.runLater(() -> items.add(filename));
        }
    }

    /**
     * Creating listView cells with images.
     * @param images images for files and folder.
     */
    private void creatingCustomCells(@NotNull Image[] images) {
        listView.setCellFactory(param -> new ListCell<String>() {
            private ImageView imageView = new ImageView();
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (!filesInDirectory.get(item)) {
                    imageView.setImage(images[0]);
                } else {
                    imageView.setImage(images[1]);
                }

                setText(item);
                setGraphic(imageView);
            }
        });
    }

    /**
     * Initializing of all objects on the scene.
     * @param scene scene, where objects will be located.
     * @param images images images images for files and folder.
     * @param stage window for directory chooser.
     * @throws URISyntaxException throw exception, when URI parsed wrong.
     */
    private void initSceneObjects(@NotNull Scene scene, @NotNull Image[] images, @NotNull Stage stage) throws URISyntaxException {
        VBox vbox = new VBox();

        HBox hBox = new HBox();
        vbox.getChildren().add(hBox);

        initConnectButton(hBox, images);
        initDisconnectButton(hBox);
        initBackButton(hBox, images);
        initDirectoryChooserButton(stage, vbox);
        initListView(images, vbox);

        ((Group) scene.getRoot()).getChildren().add(vbox);
    }

    /**
     * Initializing of List View.
     * @param images images images for files and folder.
     * @param vbox place, where listView will be located.
     */
    private void initListView(@NotNull Image[] images, @NotNull VBox vbox) {
        listView = new ListView();
        listView.setOnMouseClicked(event -> {
            // set on double click event
            if (event.getClickCount() < 2) {
                return;
            }

            String selectedItem = (String) listView.getSelectionModel().getSelectedItem();
            logger.debug("User selected {} item", selectedItem);

            if (filesInDirectory.get(selectedItem)) {
                // going to folder
                currentDirectory = currentDirectory.resolve(selectedItem);
                backButton.setDisable(false);
                updateListView(images);
            } else {
                // trying to download file
                Alert confirmDownloadDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDownloadDialog.setTitle("Confirm download");
                confirmDownloadDialog.setHeaderText(null);
                confirmDownloadDialog.setContentText("Do you like to download the file " + selectedItem + "?");

                Optional<ButtonType> result = confirmDownloadDialog.showAndWait();
                if (result.get() != ButtonType.OK) {
                    // user canceled download
                    return;
                }

                try {
                    Optional<File> response = client.executeGet(currentDirectory.resolve(selectedItem)
                                                                                .toFile().toString());

                    if (!response.isPresent()) {
                        showConnectionErrorDialog();
                    }

                    Alert successfulInformationDialog = new Alert(Alert.AlertType.INFORMATION);
                    successfulInformationDialog.setTitle("Successful");
                    successfulInformationDialog.setHeaderText(null);
                    successfulInformationDialog.setContentText("The download of " + selectedItem + " was completed successfully");

                    successfulInformationDialog.showAndWait();
                } catch (FileNotExistsException e) {
                    e.printStackTrace();
                }
            }
        });

        listView.setItems(items);
        HBox listViewHbox = new HBox(listView);
        vbox.getChildren().add(listViewHbox);
    }

    /**
     * Shows a connection error dialog to user in case of connection problems.
     */
    private void showConnectionErrorDialog() {
        Alert connectionErrorAlert = new Alert(Alert.AlertType.INFORMATION);
        connectionErrorAlert.setTitle("Error");
        connectionErrorAlert.setHeaderText(null);
        connectionErrorAlert.setContentText("Connection error");
    }

    /**
     * Initializing directory chooser with label of current downloads folder.
     * @param stage window for choosing folder.
     * @param vbox place, where chooser will be located.
     */
    private void initDirectoryChooserButton(@NotNull Stage stage, @NotNull VBox vbox) {
        labelSelectedDirectory = new Label();
        labelSelectedDirectory.setText("Selected: " + downloads.toAbsolutePath().toString());

        Button openDirectoryChooser = new Button();
        openDirectoryChooser.setText("Choose download folder");
        openDirectoryChooser.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);

            if (selectedDirectory == null) {
                downloads = standardDownloadsFolder;
                labelSelectedDirectory.setText("Selected: " + downloads.toAbsolutePath().toString());
            } else {
                labelSelectedDirectory.setText("Selected: " + selectedDirectory.toPath().toAbsolutePath().toString());
                downloads = selectedDirectory.toPath();
            }

            try {
                client.setNewDownloadsFolder(downloads);
            } catch (NotDirectoryException e) {
                e.printStackTrace();
            }
        });

        vbox.getChildren().addAll(labelSelectedDirectory, openDirectoryChooser);
    }

    /**
     * Init connect button.
     * @param pane place, where button will be located.
     * @param images images for files and folder.
     */
    private void initConnectButton(@NotNull Pane pane, @NotNull Image[] images) {
        Button connectButton = new Button("Connect to server");

        connectButton.setOnAction(actionEvent ->  {
            try {
                Optional<Pair<String, Integer>> pair = getHostInformationDialog();

                if (pair.isPresent()) {
                    client = new FileClient(pair.get().getValue(), pair.get().getKey(), downloads);
                    client.connect();
                }

                updateListView(images);
            } catch (ConnectException e) {
                showConnectionErrorDialog();
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (client == null || !client.isConnected()) {
                ((Node)(actionEvent.getSource())).getScene().getWindow().hide();
            }
        });

        pane.getChildren().add(connectButton);
    }

    private @NotNull Optional<Pair<String, Integer>> getHostInformationDialog() {
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Open connection");
        dialog.setHeaderText(null);

        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // handle user text
        TextField hostname = new TextField();
        TextField port = new TextField();

        Node connectButton = dialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        hostname.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                connectButton.setDisable(newValue.isEmpty());
            }

            try {
                Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                connectButton.setDisable(true);
            }

        });

        grid.add(new Label("Hostname :"), 0, 0);
        grid.add(hostname, 1, 0);
        grid.add(new Label("Port :"), 0, 1);
        grid.add(port, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(connectButtonType);
        loginButton.setDisable(true);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(hostname::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new Pair<>(hostname.getText(), Integer.parseInt(port.getText()));
            }

            return null;
        });

        Optional<Pair<String, Integer>> result = dialog.showAndWait();

        return result;
    }

    /**
     * Init disconnect button.
     * @param pane place, where button will be located.
     */
    private void initDisconnectButton(@NotNull Pane pane) {
        Button disconnectButton = new Button("Disconnect from server");

        disconnectButton.setOnAction(actionEvent ->  {
            try {
                client.disconnect();
            } catch (IOException | NotConnectedException e) {
                e.printStackTrace();
            }
        });

        pane.getChildren().add(disconnectButton);
    }

    /**
     * Init back button.
     * @param pane place, where button will be located.
     * @param images images for files and folder.
     */
    private void initBackButton(@NotNull Pane pane, @NotNull Image[] images) {
        backButton = new Button("Back");

        backButton.setDisable(true);
        backButton.setOnAction(actionEvent ->  {
            currentDirectory = currentDirectory.getParent();

            if (currentDirectory.equals(rootPath)) {
                backButton.setDisable(true);
            }

            updateListView(images);
        });

        pane.getChildren().add(backButton);
    }

    /**
     * Launching UI.
     */
    public static void main(@NotNull String[] args) throws IOException {
        Application.launch(args);
    }
}