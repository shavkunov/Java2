package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

public class Message {
    public static int lengthBytes = Integer.BYTES;

    private int messageLength;
    private @NotNull byte[] data;

    public Message(int messageLength, @NotNull byte[] data) {
        this.messageLength = messageLength;
        this.data = data;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public @NotNull byte[] getData() {
        return data;
    }
}