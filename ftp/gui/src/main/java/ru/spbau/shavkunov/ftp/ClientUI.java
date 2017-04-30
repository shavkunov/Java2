package ru.spbau.shavkunov.ftp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.UnknownException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static ru.spbau.shavkunov.ftp.NetworkConstants.PORT;

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
    private @Nullable Map<String, Boolean> filesInDirectory;

    /**
     * Current list view for visualize filesInDirectory.
     */
    private @Nullable ListView listView;

    /**
     * Downloads folder in user System.
     */
    private static final @NotNull Path standardDownloadsFolder = Paths.get(System.getProperty("user.home")).resolve("Downloads");

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
     * @throws UnknownException internal error of client.
     */
    private void updateListView(@NotNull Image[] images) throws UnknownException {
        filesInDirectory = client.executeList(currentDirectory.toFile().toString());


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

    private void initSceneObjects(@NotNull Scene scene, @NotNull Image[] images, @NotNull Stage stage) throws URISyntaxException {
        VBox vbox = new VBox();
        initConnectButton(vbox, images);

        ((Group) scene.getRoot()).getChildren().add(vbox);

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

        listView = new ListView();
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2) {
                return;
            }

            String item = (String) listView.getSelectionModel().getSelectedItem();
            logger.debug("User selected {} item", item);

            if (filesInDirectory.get(item)) {
                try {
                    currentDirectory = currentDirectory.resolve(item);
                    updateListView(images);
                } catch (UnknownException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);
                alertConfirm.setTitle("Confirm download");
                alertConfirm.setHeaderText(null);
                alertConfirm.setContentText("Do you like to download the file " + item + "?");

                Optional<ButtonType> result = alertConfirm.showAndWait();
                if (!(result.get() == ButtonType.OK)) {
                    return;
                }

                try {
                    client.executeGet(currentDirectory.resolve(item).toFile().toString());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("The download of " + item + " was completed successfully");

                    alert.showAndWait();
                } catch (UnknownException e) {
                    e.printStackTrace();
                }
            }
        });

        listView.setItems(items);
        HBox listViewHbox = new HBox(listView);
        vbox.getChildren().add(listViewHbox);
    }

    /**
     * Init connect to server button in vBox.
     * @param vbox place, where button will be located.
     * @param images images for files and folder.
     */
    private void initConnectButton(@NotNull VBox vbox, @NotNull Image[] images) {
        Button connectButton = new Button("Connect to server");

        connectButton.setOnAction(actionEvent ->  {
            try {
                client = new FileClient(PORT, NetworkConstants.hostname, downloads);
                client.connect();
                updateListView(images);
            } catch (UnknownException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        vbox.getChildren().add(connectButton);
    }

    public static void main(@NotNull String[] args) throws IOException {
        Application.launch(args);
    }
}