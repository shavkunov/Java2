package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.FileNotExistsException;
import ru.spbau.shavkunov.ftp.exceptions.InvalidMessageException;
import ru.spbau.shavkunov.ftp.exceptions.NotConnectedException;
import ru.spbau.shavkunov.ftp.message.Message;
import ru.spbau.shavkunov.ftp.message.MessageReader;
import ru.spbau.shavkunov.ftp.message.MessageWriter;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static ru.spbau.shavkunov.ftp.NetworkConstants.*;

/**
 * Implementation of blocking client.
 */
public class FileClient implements Client, AutoCloseable {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(FileClient.class);

    /**
     * Timeout for selecting appropriate channel.
     */
    private static final int SELECTING_TIMEOUT = 1000;

    /**
     * Downloads path
     */
    private @NotNull Path downloads;

    /**
     * Server address.
     */
    private @NotNull InetSocketAddress address;

    /**
     * Channel for communication with server.
     */
    private @Nullable SocketChannel channel;

    /**
     * Channel selector.
     */
    private @Nullable Selector selector;

    /**
     * Creating a file client.
     * @param serverPort specified port, which server is listen to.
     * @param hostname hostname of the server.
     * @param downloads folder, where download files will be located.
     * @throws IOException if an I/O error occurs.
     */
    public FileClient(int serverPort, @NotNull String hostname, @NotNull Path downloads) throws IOException {
        logger.debug("FileClient was created");

        if (!downloads.toFile().isDirectory()) {
            throw new NotDirectoryException(downloads.toString());
        }

        if (!downloads.toFile().exists()) {
            downloads.toFile().mkdir();
        }

        this.downloads = downloads;
        address = new InetSocketAddress(hostname, serverPort);
    }

    /**
     * Same as previous constructor, but downloads folder is user system download folder.
     * @param serverPort specified port, which server is listen to.
     * @param hostname hostname of the server.
     * @throws IOException if an I/O error occurs.
     */
    public FileClient(int serverPort, @NotNull String hostname) throws IOException {
        this(serverPort, hostname, standardDownloadsFolder);
    }

    @Override
    public void connect() throws IOException, ConnectException {
        logger.debug("connecting");

        selector = Selector.open();
        channel = SocketChannel.open(address);

        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_WRITE);
        logger.debug("connected to " + address);
    }

    @Override
    public void disconnect() throws NotConnectedException, IOException {
        if (channel == null || !channel.isConnected()) {
            throw new NotConnectedException();
        }

        channel.close();
    }

    @Override
    public @NotNull Optional<Map<String, Boolean>> executeList(@NotNull String path) throws FileNotExistsException {
        logger.debug("Executing list with path : {}", path);
        try {
            while (channel != null && isConnected()) {
                logger.debug("Selecting keys");
                selector.select(SELECTING_TIMEOUT);
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    logger.debug("SelectionKey :" + selectionKey);

                    if (selectionKey.isWritable()) {
                        logger.debug("handling writable");
                        byte[] content = getQueryBytes(path, LIST_QUERY);
                        MessageWriter writer = new MessageWriter(new Message(content), channel);

                        while (!writer.isCompleted()) {
                            logger.debug("Sending message to server");
                            writer.sendMessage();
                        }

                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }

                    if (selectionKey.isReadable()) {
                        logger.debug("handling readable");
                        MessageReader reader = new MessageReader(channel);
                        Message message = reader.readNow();
                        logger.debug("Read response from server");
                        selectionKey.interestOps(SelectionKey.OP_WRITE);

                        if (message.compareTo(EMPTY_MESSAGE) == 0) {
                            throw new FileNotExistsException();
                        }

                        return Optional.of(parseListMessage(message));
                    }

                    iterator.remove();
                }
            }
        } catch (IOException | InvalidMessageException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Serializing query.
     * @param path path of query.
     * @param typeOfQuery GET_QUERY or LIST_QUERY constants.
     * @return serialized query message into byte array.
     */
    private @NotNull byte[] getQueryBytes(@NotNull String path, int typeOfQuery) {
        byte[] content = null;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeInt(typeOfQuery);
            output.writeObject(path);

            output.flush();
            content = byteArrayOutputStream.toByteArray();

            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // should not happen
        return content;
    }

    /**
     * Deserializer of server list response.
     * @param message response of the server.
     * @return list of filenames of the asked server directory.
     * Each entry consist of filename and boolean value. It's true if filename is directory and false otherwise.
     */
    private @NotNull Map<String, Boolean> parseListMessage(@NotNull Message message) {
        Map<String, Boolean> parsedMessage = new HashMap<>();
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message.getData());
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            int size = input.readInt();
            for (int i = 0; i < size; i++) {
                String filename = (String) input.readObject();
                boolean isDir = input.readBoolean();
                parsedMessage.put(filename, isDir);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return parsedMessage;
    }

    @Override
    public @NotNull Optional<File> executeGet(@NotNull String pathToFile) throws FileNotExistsException {
        logger.debug("Executing get with {}", pathToFile);
        try {
            Path path = Paths.get(pathToFile);

            while (channel != null && isConnected()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isWritable()) {
                        byte[] content = getQueryBytes(pathToFile, GET_QUERY);
                        MessageWriter writer = new MessageWriter(new Message(content), channel);

                        while (!writer.isCompleted()) {
                            writer.sendMessage();
                        }

                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }

                    if (selectionKey.isReadable()) {
                        Path pathToLocalCopy = downloads.resolve(path.toFile().getName());


                        ByteBuffer length = ByteBuffer.allocate(Message.longLengthBytes);

                        logger.debug("Getting file size");
                        while (length.hasRemaining()) {
                            channel.read(length);
                        }

                        length.flip();
                        long fileSize = length.getLong();
                        logger.debug("File size is {}", fileSize);

                        if (fileSize == -1) {
                            throw new FileNotExistsException();
                        }

                        FileChannel fileChannel = FileChannel.open(pathToLocalCopy, StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING);

                        while (fileSize > 0) {
                            long receivedBytes = fileChannel.transferFrom(channel, fileChannel.position(), fileSize);
                            fileChannel.position(fileChannel.position() + receivedBytes);
                            logger.debug("Received bytes : {}", receivedBytes);
                            fileSize -= receivedBytes;
                            logger.debug("Size remaining : {}", fileSize);
                        }

                        logger.debug("File is downloaded");
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                        return Optional.of(pathToLocalCopy.toFile());
                    }
                    iterator.remove();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public void setNewDownloadsFolder(@NotNull Path path) throws NotDirectoryException {
        if (path.toFile().isDirectory()) {
            downloads = path;
            return;
        }

        throw new NotDirectoryException(path.toString());
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public void close() throws Exception {
        channel.close();
        selector.close();
    }
}