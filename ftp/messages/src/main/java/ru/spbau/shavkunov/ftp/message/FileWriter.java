package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileWriter implements Writer {
    private static int BLOCK_SIZE = 4096;
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.longLengthBytes);
    private @NotNull WritableByteChannel clientChannel;
    private @NotNull FileChannel fileChannel;
    private boolean isSent = false;
    private boolean isLengthSent = false;

    public FileWriter(@NotNull Path path, @NotNull WritableByteChannel channel) throws IOException {
        clientChannel = channel;
        fileChannel = FileChannel.open(path, StandardOpenOption.READ);

        lengthBuffer.putLong(fileChannel.size());
        lengthBuffer.flip();
    }

    @Override
    public void sendMessage() throws IOException {
        if (!isLengthSent) {
            clientChannel.write(lengthBuffer);

            if (lengthBuffer.hasRemaining()) {
                return;
            }

            isLengthSent = true;
        }

        long sentBytes = fileChannel.transferTo(fileChannel.position(), BLOCK_SIZE, clientChannel);
        fileChannel.position(fileChannel.position() + sentBytes);

        if (fileChannel.position() == fileChannel.size()) {
            isSent = true;
            fileChannel.close();
        }
    }

    @Override
    public boolean isCompleted() {
        return isSent && isLengthSent;
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
        clientChannel.close();
    }
}
