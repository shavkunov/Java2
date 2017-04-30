package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.InvalidMessageException;
import ru.spbau.shavkunov.ftp.exceptions.UnknownException;
import ru.spbau.shavkunov.ftp.message.Message;
import ru.spbau.shavkunov.ftp.message.MessageReader;
import ru.spbau.shavkunov.ftp.message.MessageWriter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ru.spbau.shavkunov.ftp.NetworkConstants.GET_QUERY;
import static ru.spbau.shavkunov.ftp.NetworkConstants.LIST_QUERY;

public class FileClient implements Client {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(FileClient.class);
    private static final int SELECTING_TIMEOUT = 1000;

    private @NotNull Path downloads;
    private @NotNull InetSocketAddress address;
    private @Nullable SocketChannel channel;
    private @Nullable Selector selector;

    public FileClient(int serverPort, @NotNull String hostname, @NotNull Path downloads) throws IOException {
        logger.debug("FileClient was created");
        // TODO : check for valid download path
        downloads.toFile().mkdir();
        this.downloads = downloads;
        address = new InetSocketAddress(hostname, serverPort);
    }

    @Override
    public void connect() throws IOException {
        logger.debug("connecting");

        selector = Selector.open();
        channel = SocketChannel.open(address);
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_WRITE);
        logger.debug("connected to " + address);
    }

    @Override
    public @NotNull Map<String, Boolean> executeList(@NotNull String path) throws UnknownException {
        logger.debug("Executing list with path : {}", path);
        try {
            while (true) {
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
                        return parseListMessage(message);
                    }

                    iterator.remove();
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidMessageException e) {
            e.printStackTrace();
        }

        throw new UnknownException();
    }

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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return parsedMessage;
    }

    @Override
    public @NotNull File executeGet(@NotNull String pathToFile) throws UnknownException {
        logger.debug("Executing get with {}", pathToFile);
        try {
            Path path = Paths.get(pathToFile);

            while (true) {
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
                        pathToLocalCopy.toFile().createNewFile();

                        FileChannel fileChannel = FileChannel.open(pathToLocalCopy, StandardOpenOption.WRITE,
                                                                                    StandardOpenOption.TRUNCATE_EXISTING);


                        ByteBuffer length = ByteBuffer.allocate(Message.longLengthBytes);

                        while (length.hasRemaining()) {
                            channel.read(length);
                        }

                        length.flip();
                        long fileSize = length.getLong();

                        long receivedBytes = fileChannel.transferFrom(channel, 0, fileSize);
                        while (receivedBytes != fileSize) {
                            receivedBytes = fileChannel.transferFrom(channel, receivedBytes, fileSize);
                        }

                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                        return pathToLocalCopy.toFile();
                    }
                    iterator.remove();
                }

            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new UnknownException();
    }
}