package ru.spbau.shavkunov.ftp.message;

import org.jetbrains.annotations.NotNull;

public class Message {
    public static int intLengthBytes = Integer.BYTES;
    public static int longLengthBytes = Long.BYTES;

    private @NotNull byte[] data;

    public Message(@NotNull byte[] data) {
        this.data = data;
    }

    public int getMessageLength() {
        return data.length;
    }

    public @NotNull byte[] getData() {
        return data;
    }
}