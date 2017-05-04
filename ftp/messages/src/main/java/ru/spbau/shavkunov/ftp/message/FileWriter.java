package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Class responsible for sending file by block portions.
 */
public class FileWriter implements Writer {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(FileWriter.class);
    /**
     * Size of each block.
     */
    private static int BLOCK_SIZE = 4096;

    /**
     * Buffer for sending length.
     */
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.longLengthBytes);

    /**
     * Client channel.
     */
    private @NotNull WritableByteChannel clientChannel;

    /**
     * Channel for file.
     */
    private @Nullable FileChannel fileChannel;

    /**
     * Status sending flag.
     */
    private boolean isSent = false;

    /**
     * Status sending length flag.
     */
    private boolean isLengthSent = false;

    /**
     * Creating file writer. It will be able to send file to client.
     * @param path path to file.
     * @param channel client channel.
     * @throws IOException if an I/O error occurs.
     */
    public FileWriter(@NotNull Path path, @NotNull WritableByteChannel channel) throws IOException {
        clientChannel = channel;
        fileChannel = FileChannel.open(path, StandardOpenOption.READ);

        lengthBuffer.putLong(fileChannel.size());
        logger.debug("File size is {}", fileChannel.size());
        lengthBuffer.flip();

        logger.debug("Created file writer");
    }

    /**
     * Creating writer for writing empty message.
     * @param channel client channel.
     */
    public FileWriter(@NotNull WritableByteChannel channel) {
        clientChannel = channel;

        lengthBuffer.putLong(-1);
        lengthBuffer.flip();
    }

    @Override
    public void sendMessage() throws IOException {
        if (!isLengthSent) {
            logger.debug("Sending length of file");
            clientChannel.write(lengthBuffer);

            if (lengthBuffer.hasRemaining()) {
                return;
            }

            isLengthSent = true;
            logger.debug("Length of file is sent");

            if (fileChannel == null) {
                isSent = true;
                return;
            }
        }

        logger.debug("Sending file content");
        long sentBytes = fileChannel.transferTo(fileChannel.position(), BLOCK_SIZE, clientChannel);
        logger.debug("Sent bytes : {}", sentBytes);
        fileChannel.position(fileChannel.position() + sentBytes);

        if (fileChannel.position() == fileChannel.size()) {
            logger.debug("File content is sent to client");
            isSent = true;
        }
    }

    @Override
    public boolean isCompleted() {
        return isSent && isLengthSent;
    }

    @Override
    public void close() throws IOException {
        if (fileChannel != null) {
            fileChannel.close();
        }

        clientChannel.close();
    }
}
