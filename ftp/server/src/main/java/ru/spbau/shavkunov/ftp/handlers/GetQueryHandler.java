package ru.spbau.shavkunov.ftp.handlers;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.ftp.message.FileResponse;
import ru.spbau.shavkunov.ftp.message.Response;

import java.io.IOException;
import java.nio.file.Path;

public class GetQueryHandler {
    @NotNull Path path;

    public GetQueryHandler(@NotNull Path path) {
        this.path = path;
    }

    public @NotNull Response handleGetQuery() throws IOException {
        return new FileResponse(path);
    }
}