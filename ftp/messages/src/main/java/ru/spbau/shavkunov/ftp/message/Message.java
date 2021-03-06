package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Class representing a message, which can be read or written.
 */
public class Message implements Comparable<Message> {
    /**
     * Size of int length buffer.
     */
    public static int intLengthBytes = Integer.BYTES;

    /**
     * Size of long length buffer.
     */
    public static int longLengthBytes = Long.BYTES;

    /**
     * Content of the message.
     */
    private @NotNull byte[] data;

    /**
     * Creating message with content.
     * @param data content representing message.
     */
    public Message(@NotNull byte[] data) {
        this.data = data;
    }

    /**
     * Length of the message.
     * @return returns length of the message.
     */
    public int getMessageLength() {
        return data.length;
    }

    /**
     * Content of the message.
     * @return returns content of the message.
     */
    public @NotNull byte[] getData() {
        return data;
    }

    @Override
    public int compareTo(Message o) {
        if (Arrays.equals(data, o.getData())) {
            return 0;
        }

        return 1;
    }
}