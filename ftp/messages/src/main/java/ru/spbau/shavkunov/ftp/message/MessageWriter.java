package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class MessageWriter implements Writer {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(MessageWriter.class);
    private @NotNull WritableByteChannel channel;
    private @NotNull ByteBuffer buffer;

    public MessageWriter(@NotNull Message message, @NotNull WritableByteChannel channel) {
        this.channel = channel;

        buffer = ByteBuffer.allocate(Integer.BYTES + message.getMessageLength());
        buffer.putInt(message.getMessageLength());
        buffer.put(message.getData());
        buffer.flip();

        logger.debug("Writer was created with channel " + channel);
    }

    @Override
    public void sendMessage() throws IOException {
        logger.debug("Sending message");
        channel.write(buffer);

        if (isCompleted()) {
            logger.debug("Data has sent");
        }
    }

    @Override
    public boolean isCompleted() {
        return !buffer.hasRemaining();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}