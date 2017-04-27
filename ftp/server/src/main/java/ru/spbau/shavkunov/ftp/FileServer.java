package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.InvalidMessageException;
import ru.spbau.shavkunov.ftp.exceptions.InvalidQueryException;
import ru.spbau.shavkunov.ftp.handlers.GetQueryHandler;
import ru.spbau.shavkunov.ftp.handlers.ListQueryHandler;
import ru.spbau.shavkunov.ftp.message.Message;
import ru.spbau.shavkunov.ftp.message.MessageReader;
import ru.spbau.shavkunov.ftp.message.MessageWriter;
import ru.spbau.shavkunov.ftp.message.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

// TODO : надо сделать отдельный поток.
public class FileServer implements Server {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(FileServer.class);
    private static final int SELECTING_TIMEOUT = 1000;

    private boolean isRunning = false;
    private @NotNull Selector selector;
    private @NotNull ServerSocketChannel serverChannel;

    public FileServer(int port) throws IOException {
        createServerFiles();
        InetSocketAddress socketAddress = new InetSocketAddress(NetworkConstants.hostname, port);
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(socketAddress);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.debug("Server created on address : " + socketAddress.getAddress());
    }

    private void createServerFiles() {
        Path rootPath = Paths.get(".").normalize();

        try {
            Files.write(rootPath.resolve("test1"), "text1".getBytes());
            Files.write(rootPath.resolve("test2"), "text2".getBytes());
            rootPath.resolve("test").toFile().mkdir();
            Files.write(rootPath.resolve("test").resolve("test3"), "test3".getBytes());
            Files.write(rootPath.resolve("test").resolve("test4"), "test4".getBytes());
            Files.write(rootPath.resolve("test").resolve("test5"), "test5".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        isRunning = true;
        run();
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    private void run() {
        try {
            while (isRunning) {
                logger.debug("Selecting keys");
                selector.select(SELECTING_TIMEOUT);
                logger.debug("Size: {}", selector.keys().size());
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    keyIterator.remove();
                    if (selectionKey.isAcceptable()) {
                        logger.debug("Selected acceptable key : " + selectionKey.toString());
                        handleAcceptable(selectionKey);
                    }

                    if (selectionKey.isReadable()) {
                        logger.debug("Selected readable key : " + selectionKey.toString());
                        handleReadable(selectionKey);
                    }

                    if (selectionKey.isWritable()) {
                        logger.debug("Selected writable key : " + selectionKey.toString());
                        handleWritable(selectionKey);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidQueryException e) {
            // сообщить пользователю об ошибке его запроса.
        } catch (InvalidMessageException e) {
            // сообщить пользователю об неккоректном запросе.
        }
    }

    private void handleAcceptable(@NotNull SelectionKey selectionKey) throws IOException {
        logger.debug("handling acceptable");
        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverChannel.accept();
        logger.debug("got client socket channel :" + socketChannel.toString());

        socketChannel.configureBlocking(false);
        SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ);
        MessageReader reader = new MessageReader(clientKey);
        clientKey.attach(reader);
    }

    private void handleReadable(@NotNull SelectionKey selectionKey) throws IOException, InvalidQueryException,
                                                                                InvalidMessageException {
        logger.debug("handling readable");
        MessageReader reader = (MessageReader) selectionKey.attachment();
        Optional<Message> message = reader.readMessage();

        if (message.isPresent()) {
            logger.debug("Message is read");
            Response response = handleUserTask(message.get().getData());
            MessageWriter writer = new MessageWriter(response, selectionKey);
            selectionKey.channel().register(selectionKey.selector(), SelectionKey.OP_WRITE);
            selectionKey.attach(writer);
        }
    }

    private void handleWritable(@NotNull SelectionKey selectionKey) throws IOException {
        logger.debug("handling writable");
        MessageWriter writer = (MessageWriter) selectionKey.attachment();
        writer.sendMessage();
        if (writer.isCompleted()) {
            logger.debug("Writer wrote message");
            selectionKey.interestOps(0);
        }
    }

    private @NotNull Response handleUserTask(@NotNull byte[] content) throws InvalidQueryException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            int typeOfQuery = input.readInt();
            Path path = Paths.get((String) input.readObject());

            if (typeOfQuery == NetworkConstants.LIST_QUERY) {
                return new ListQueryHandler(path).handleListQuery();
            }
            if (typeOfQuery == NetworkConstants.GET_QUERY) {
                return new GetQueryHandler(path).handleGetQuery();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        throw new InvalidQueryException();
    }
}