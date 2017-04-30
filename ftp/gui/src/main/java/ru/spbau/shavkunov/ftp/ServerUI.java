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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static ru.spbau.shavkunov.ftp.NetworkConstants.PORT;

public class ServerUI extends Application {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(ServerUI.class);
    private static @NotNull Path rootPath = Paths.get("test"); // temporary
    private @Nullable Client client;
    private @NotNull volatile Path path = rootPath;
    private @Nullable Map<String, Boolean> filesInDirectory;
    private @Nullable ListView listView;
    private @NotNull Path downloads = Paths.get(System.getProperty("user.home")).resolve("Downloads");
    private @NotNull ObservableList<String> items = FXCollections.observableArrayList();
    private @Nullable Label labelSelectedDirectory;

    @Override
    public void start(@NotNull Stage stage) throws Exception {
        Server server = new FileServer(PORT);
        server.start();

        stage.setTitle("Ftp server");
        Scene startScene = new Scene(new Group());

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("file.png").getFile());
        File folder = new File(classLoader.getResource("folder.png").getFile());

        Image fileImage = new Image(file.toURI().toString(), 32, 32, true, true, true);
        Image folderImage = new Image(folder.toURI().toString(), 32, 32, true, true, true);
        Image[] images = {fileImage, folderImage};

        initButtons(startScene, images, stage);

        stage.setScene(startScene);
        stage.show();
    }

    public void initListView(@NotNull Image[] images) throws UnknownException {
        filesInDirectory = client.executeList(path.toFile().toString());

        if (filesInDirectory.size() > 0) {
            items.clear();
        } else {
            path = path.getParent();
        }

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

        for (String filename : filesInDirectory.keySet()) {
            Platform.runLater(() -> items.add(filename));
        }
    }

    public void initButtons(@NotNull Scene scene, @NotNull Image[] images, @NotNull Stage stage) throws URISyntaxException {
        VBox vbox = new VBox();

        Button connectButton = new Button("Connect to server");
        connectButton.setOnAction(actionEvent ->  {
            try {
                client = new FileClient(PORT, NetworkConstants.hostname, downloads);
                client.connect();
                initListView(images);
            } catch (UnknownException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        vbox.getChildren().add(connectButton);
        ((Group) scene.getRoot()).getChildren().add(vbox);

        labelSelectedDirectory = new Label();
        labelSelectedDirectory.setText("Selected: " + downloads.toAbsolutePath().toString());

        Button openDirectoryChooser = new Button();
        openDirectoryChooser.setText("Choose download folder");
        openDirectoryChooser.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);

            if (selectedDirectory == null) {
                labelSelectedDirectory.setText("Selected: " + downloads.toAbsolutePath().toString());
            } else {
                labelSelectedDirectory.setText("Selected: " + selectedDirectory.toPath().toAbsolutePath().toString());
                downloads = selectedDirectory.toPath();
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
                    path = path.resolve(item);
                    initListView(images);
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
                    client.executeGet(path.resolve(item).toFile().toString());

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

    public static void main(@NotNull String[] args) throws IOException {
        Application.launch(args);
    }
}