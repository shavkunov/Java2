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
import ru.spbau.shavkunov.ftp.message.Writer;

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

public class FileServer implements Server {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(FileServer.class);
    private static final int SELECTING_TIMEOUT = 1000;

    private volatile boolean isRunning = false;
    private @NotNull Thread serverThread;

    public FileServer(int port) throws IOException {
        serverThread = new Thread(new RunningService(port));
    }

    private void createServerFiles() {
        Path rootPath = Paths.get("test").normalize();
        rootPath.toFile().mkdir();

        try {
            Files.write(rootPath.resolve("test1"), "text1".getBytes());
            Files.write(rootPath.resolve("test2"), "text2".getBytes());

            rootPath.resolve("dir").toFile().mkdir();
            Files.write(rootPath.resolve("dir").resolve("test3"), "test3".getBytes());
            Files.write(rootPath.resolve("dir").resolve("test4"), "test4".getBytes());
            Files.write(rootPath.resolve("dir").resolve("test5"), "test5".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        isRunning = true;
        serverThread.start();
    }

    @Override
    public void stop() {
        isRunning = false;
        serverThread.interrupt();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class RunningService implements Runnable {
        private @NotNull Selector selector;

        public RunningService(int port) throws IOException {
            createServerFiles();
            InetSocketAddress socketAddress = new InetSocketAddress(NetworkConstants.hostname, port);
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(socketAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.debug("Server created on address : " + socketAddress.getAddress());
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    logger.debug("Selecting keys");
                    selector.select(SELECTING_TIMEOUT);
                    logger.debug("Size: {}", selector.keys().size());
                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();

                        if (selectionKey.isAcceptable()) {
                            logger.debug("Selected acceptable key : " + selectionKey.toString());
                            handleAcceptable(selectionKey);
                        }

                        if (!selectionKey.isValid()) {
                            logger.debug("Client {} was disconnected", selectionKey.channel());
                            continue;
                        }

                        if (selectionKey.isReadable()) {
                            logger.debug("Selected readable key : " + selectionKey.toString());
                            handleReadable(selectionKey);
                        } else {
                            if (selectionKey.isWritable()) {
                                logger.debug("Selected writable key : " + selectionKey.toString());
                                handleWritable(selectionKey);
                            }
                        }

                        keyIterator.remove();
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
            MessageReader reader = new MessageReader((SocketChannel) clientKey.channel());
            clientKey.attach(reader);
        }

        private void handleReadable(@NotNull SelectionKey selectionKey) throws IOException, InvalidQueryException,
                InvalidMessageException {
            logger.debug("handling readable");
            MessageReader reader = (MessageReader) selectionKey.attachment();

            if (reader.isDisconnected()) {
                logger.debug("Client {} was disconnected", selectionKey.channel());
                selectionKey.channel().close();
                return;
            }

            Optional<Message> message = reader.readMessage();

            if (message.isPresent()) {
                logger.debug("Message is read");
                SocketChannel userChannel = (SocketChannel) selectionKey.channel();
                Writer writer = handleUserTask(message.get(), userChannel);
                selectionKey.channel().register(selectionKey.selector(), SelectionKey.OP_WRITE);
                selectionKey.attach(writer);
            }
        }

        private void handleWritable(@NotNull SelectionKey selectionKey) throws IOException {
            logger.debug("handling writable");
            Writer writer = (Writer) selectionKey.attachment();
            try {
                writer.sendMessage();
                if (writer.isCompleted()) {
                    logger.debug("Writer wrote message");

                    selectionKey.channel().register(selector, SelectionKey.OP_READ);
                    MessageReader reader = new MessageReader((SocketChannel) selectionKey.channel());
                    selectionKey.attach(reader);
                }
            } catch (IOException e) {
                writer.close();
                logger.debug("Client {} was disconnected", selectionKey.channel());
            }
        }

        private @NotNull Writer handleUserTask(@NotNull Message message, @NotNull SocketChannel userChannel)
                throws InvalidQueryException {
            logger.debug("handling user query");
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message.getData());
                 ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

                int typeOfQuery = input.readInt();
                Path path = Paths.get((String) input.readObject());

                if (typeOfQuery == NetworkConstants.LIST_QUERY) {
                    logger.debug("User asked for list directory");
                    return new ListQueryHandler(path).handleListQuery(userChannel);
                }
                if (typeOfQuery == NetworkConstants.GET_QUERY) {
                    logger.debug("User asked for get file");
                    return new GetQueryHandler(path).handleGetQuery(userChannel);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            throw new InvalidQueryException();
        }
    }
}