package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface Response {
    boolean isSent();
    void sendData(@NotNull SocketChannel channel) throws IOException;
    long length();
}