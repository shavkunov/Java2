package ru.spbau.shavkunov.ftp.message;

import java.io.IOException;

/**
 * Interface of object, which is supposed to write Message.
 */
public interface Writer {
    /**
     * Writer trying to send part of message.
     * @throws IOException if an I/O error occurs.
     */
    void sendMessage() throws IOException;

    /**
     * Returns status of writing message.
     * @return true, if writer is already sent the message and false otherwise.
     */
    boolean isCompleted();

    /**
     * Closing internal resources.
     * @throws IOException if an I/O error occurs.
     */
    void close() throws IOException;
}
