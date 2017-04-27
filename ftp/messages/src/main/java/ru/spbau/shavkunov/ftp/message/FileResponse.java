package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class FileResponse implements Response {
    private static int BLOCK_SIZE = 4096;
    private boolean isSent = false;
    private @NotNull Path path;
    private @Nullable ByteBuffer bucket = ByteBuffer.allocate(BLOCK_SIZE);
    private @NotNull InputStream inputStream;

    public static int getBlockSize() {
        return BLOCK_SIZE;
    }

    public FileResponse(@NotNull Path path) throws IOException {
        this.path = path;

        inputStream = new FileInputStream(path.toFile());
        getNextBlock();
    }

    private void getNextBlock() throws IOException {
        byte[] bytes = new byte[BLOCK_SIZE];
        if (inputStream.read(bytes) != -1) {
            bucket = ByteBuffer.wrap(bytes);
        }

        bucket = null;
    }

    @Override
    public boolean isSent() {
        return isSent;
    }

    @Override
    public void sendData(@NotNull SocketChannel channel) throws IOException {
        SocketChannel socketChannel = channel;
        socketChannel.write(bucket);

        if (!bucket.hasRemaining()) {
            getNextBlock();
            if (bucket == null) {
                isSent = true;
            }
        }
    }

    @Override
    public long length() {
        if (!path.toFile().exists()) {
            return 0;
        }

        return path.toFile().length();
    }
}