package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MessageWriter {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(MessageWriter.class);
    private @NotNull SocketChannel channel;
    private @NotNull Response response;
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    private boolean isSentResponseLength = false;

    public MessageWriter(@NotNull Response response, @NotNull SelectionKey selectionKey) {
        this.channel = (SocketChannel) selectionKey.channel();
        this.response = response;

        lengthBuffer.asLongBuffer().put(response.length());
        logger.debug("Writer was created with selectionKey " + selectionKey);
    }

    public void sendMessage() throws IOException {
        logger.debug("Sending message");
        if (!isSentResponseLength) {
            sendLength();

            if (!isSentResponseLength) {
                return;
            }
        }

        response.sendData(channel);
    }

    private void sendLength() throws IOException {
        logger.debug("Sending length");
        SocketChannel socketChannel = channel;
        socketChannel.write(lengthBuffer);

        if (!lengthBuffer.hasRemaining()) {
            isSentResponseLength = true;
            logger.debug("Sent length");
        }
    }

    public boolean isCompleted() {
        return isSentResponseLength && response.isSent();
    }
}