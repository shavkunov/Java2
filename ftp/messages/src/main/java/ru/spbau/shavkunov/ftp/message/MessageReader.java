package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.InvalidMessageException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class MessageReader {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(MessageReader.class);
    private @NotNull SocketChannel client;
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.lengthBytes);
    private int size = -1;
    private @Nullable ByteBuffer dataBuffer;

    public MessageReader(@NotNull SelectionKey selectionKey) {
        logger.debug("Reader was created with " + selectionKey.toString());
        client = (SocketChannel) selectionKey.channel();
    }

    public @NotNull Optional<Message> readMessage() throws IOException, InvalidMessageException {
        if (lengthBuffer.hasRemaining()) {
            logger.debug("Reading length buffer.");
            client.read(lengthBuffer);

            if (!lengthBuffer.hasRemaining()) {
                lengthBuffer.flip();
                size = lengthBuffer.getInt();

                logger.debug("Read length buffer. Size = " + size);
                if (size == 0) {
                    throw new InvalidMessageException();
                }
            }
        }

        if (lengthBuffer.hasRemaining()) {
            return Optional.empty();
        }

        if (dataBuffer == null) {
            dataBuffer = ByteBuffer.allocate(size);
        }

        logger.debug("Reading data buffer");
        client.read(dataBuffer);
        if (dataBuffer.hasRemaining()) {
            return Optional.empty();
        }

        dataBuffer.flip();
        byte[] content = new byte[size];
        dataBuffer.get(content);

        logger.debug("Read data from " + client);
        return Optional.of(new Message(size, content));
    }

    public @NotNull Message readNow() throws IOException, InvalidMessageException {
        Optional<Message> answer = Optional.empty();

        while (!answer.isPresent()) {
            answer = readMessage();
        }

        return answer.get();
    }
}