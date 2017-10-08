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

/**
 * Class for reading messages from readable channels.
 */
public class MessageReader {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(MessageReader.class);

    /**
     * Readable channel for getting information.
     */
    private @NotNull ReadableByteChannel client;

    /**
     * Length buffer for reading length of message.
     */
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.intLengthBytes);

    /**
     * Length of the message.
     */
    private int size = -1;

    /**
     * Buffer for reading message.
     */
    private @Nullable ByteBuffer dataBuffer;

    /**
     * Disconnected status flag.
     */
    private boolean isDisconnected;

    /**
     * Creating reader from readable channel.
     * @param channel client channel.
     */
    public MessageReader(@NotNull ReadableByteChannel channel) {
        logger.debug("Reader was created with channel " + channel);
        client = channel;

        isDisconnected = false;
    }

    /**
     * Attempt to read message from channel. It will read in non-blocking mode.
     * @return Optional.empty if message isn't read yet and otherwise is will returns optional with message.
     * @throws IOException if an I/O error occurs.
     * @throws InvalidMessageException if client is sent invalid message.
     */
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

    /**
     * Blocking read message. It will read until all message isn't read.
     * @return Message from client.
     * @throws IOException if an I/O error occurs.
     * @throws InvalidMessageException if client is sent invalid message.
     */
    public @NotNull Message readNow() throws IOException, InvalidMessageException {
        Optional<Message> answer = Optional.empty();

        while (!answer.isPresent()) {
            answer = readMessage();
        }

        return answer.get();
    }

    /**
     * Returns disconnected status flag.
     * @return disconnected status flag.
     */
    public boolean isDisconnected() {
        return isDisconnected;
    }
}