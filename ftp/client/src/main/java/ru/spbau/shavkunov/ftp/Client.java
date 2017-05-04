package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.ftp.exceptions.FileNotExistsException;
import ru.spbau.shavkunov.ftp.exceptions.NotConnectedException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Interface of server's client.
 */
public interface Client {
    /**
     * Connect to server.
     * @throws IOException if an I/O error occurs.
     */
    void connect() throws IOException, ConnectException;

    /**
     * Disconnect from server.
     * @throws NotConnectedException if invoking when client isn't connected to server.
     * @throws IOException if an I/O error occurs.
     */
    void disconnect() throws NotConnectedException, IOException;

    /**
     * Executing list query. It returns map of files in specified directory of the server.
     * Each entry consist of filename and boolean value. It's true if filename is directory and false otherwise.
     * @param path specified path in server where list query will be executed.
     * @return map of files in directory or Optional.empty if client is disconnected.
     */
    @NotNull Optional<Map<String, Boolean>> executeList(@NotNull String path) throws FileNotExistsException;

    /**
     * Executing get query. It will download specified file into downloads path.
     * @param path specified file of the server.
     * @return File downloaded into downloads path or Optional.empty if client is disconnected.
     * @throws FileNotExistsException if client asks to download a directory or not existing file.
     */
    @NotNull Optional<File> executeGet(@NotNull String path) throws FileNotExistsException;

    /**
     * Default value is System downloads path. This method sets directory as download folder.
     * @param path path to directory, which will be set as download folder
     * @throws NotDirectoryException if provided path isn't a directory.
     */
    void setNewDownloadsFolder(@NotNull Path path) throws NotDirectoryException;

    /**
     * Returns true if client connected to server.
     * @return true if client connected to server.
     */
    boolean isConnected();
}