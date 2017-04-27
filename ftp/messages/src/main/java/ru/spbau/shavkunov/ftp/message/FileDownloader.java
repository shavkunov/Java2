package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDownloader {
    private @NotNull SocketChannel channel;
    private @NotNull ByteBuffer lengthBuffer = ByteBuffer.allocate(Message.lengthBytes);
    private @NotNull ByteBuffer dataBuffer = ByteBuffer.allocate(FileResponse.getBlockSize());
    private @NotNull Path destination;

    public FileDownloader(@NotNull SelectionKey selectionKey, @NotNull String path, @NotNull Path destination) throws IOException {
        this.destination = destination.resolve(Paths.get(path).toFile().getName());
        channel = (SocketChannel) selectionKey.channel();
    }

    public File downloadFile() throws IOException {
        destination.toFile().createNewFile();

        while (lengthBuffer.hasRemaining()) {
            channel.read(lengthBuffer);
        }

        lengthBuffer.flip();
        int fileLength = lengthBuffer.getInt();

        try (FileOutputStream output = new FileOutputStream(destination.toFile(), true)) {
            while (fileLength > 0) {
                while (dataBuffer.hasRemaining()) {
                    channel.read(dataBuffer);
                }

                dataBuffer.flip();
                output.write(dataBuffer.array());
                dataBuffer.rewind();

                fileLength -= FileResponse.getBlockSize();
            }
        }

        return destination.toFile();
    }
}