package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.ftp.exceptions.UnknownException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface Client {
    void connect() throws IOException;
    @NotNull Map<String, Boolean> executeList(@NotNull String path) throws UnknownException;
    @NotNull File executeGet(@NotNull String path) throws UnknownException;
}