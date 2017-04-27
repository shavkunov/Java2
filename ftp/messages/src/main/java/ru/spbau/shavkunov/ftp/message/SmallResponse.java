package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SmallResponse implements Response {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(SmallResponse.class);
    private @NotNull ByteBuffer byteBuffer;

    public SmallResponse(@NotNull byte[] content) {
        byteBuffer = ByteBuffer.wrap(content);
        logger.debug("Small response was created with content " + content);
    }

    @Override
    public boolean isSent() {
        return byteBuffer.hasRemaining();
    }

    @Override
    public void sendData(@NotNull SocketChannel channel) throws IOException {
        logger.debug("Small response sending data");
        channel.write(byteBuffer);

        if (isSent()) {
            logger.debug("Small response sent data");
            byteBuffer.rewind();
        }
    }

    @Override
    public long length() {
        return byteBuffer.capacity();
    }
}