package ru.spbau.shavkunov.ftp.message;

import java.io.IOException;

public interface Writer {
    void sendMessage() throws IOException;
    boolean isCompleted();
    void close() throws IOException;
}
