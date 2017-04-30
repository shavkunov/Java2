package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.exceptions.InvalidMessageException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

public class MessageReader {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(MessageReader.class);
    private @NotNull ReadableByteChannel client;
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.intLengthBytes);
    private int size = -1;
    private @Nullable ByteBuffer dataBuffer;
    private boolean isDisconnected;

    public MessageReader(@NotNull ReadableByteChannel channel) {
        logger.debug("Reader was created with channel " + channel);
        client = channel;

        isDisconnected = false;
    }

    public @NotNull Optional<Message> readMessage() throws IOException, InvalidMessageException {
        if (lengthBuffer.hasRemaining()) {
            logger.debug("Reading length buffer.");
            if (client.read(lengthBuffer) == -1) {
                isDisconnected = true;
                return Optional.empty();
            }

            if (!lengthBuffer.hasRemaining()) {
                lengthBuffer.flip();
                size = lengthBuffer.getInt();

                logger.debug("Length buffer had read. Size = " + size);
                if (size == 0) {
                    throw new InvalidMessageException();
                }
            } else {
                return Optional.empty();
            }
        }

        if (dataBuffer == null) {
            dataBuffer = ByteBuffer.allocate(size);
        }

        logger.debug("Reading data buffer");
        if (client.read(dataBuffer) == -1) {
            isDisconnected = true;
            return Optional.empty();
        }
        if (dataBuffer.hasRemaining()) {
            logger.debug("data buffer has remaining");
            return Optional.empty();
        }

        dataBuffer.flip();
        byte[] content = new byte[size];
        dataBuffer.get(content);

        logger.debug("Read data from " + client);
        return Optional.of(new Message(content));
    }

    public @NotNull Message readNow() throws IOException, InvalidMessageException {
        Optional<Message> answer = Optional.empty();

        while (!answer.isPresent()) {
            answer = readMessage();
        }

        return answer.get();
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }
}