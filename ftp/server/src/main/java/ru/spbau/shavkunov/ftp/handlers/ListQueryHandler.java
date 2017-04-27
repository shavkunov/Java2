package ru.spbau.shavkunov.ftp.handlers;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.ftp.message.SmallResponse;
import ru.spbau.shavkunov.ftp.message.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;

public class ListQueryHandler {
    private @NotNull Path path;

    public ListQueryHandler(@NotNull Path path) {
        this.path = path;
    }

    public @NotNull Response handleListQuery() {
        byte[] content = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            if (path.toFile().exists()) {
                for (File file : path.toFile().listFiles()) {
                    output.writeObject(file.getName());
                    output.writeBoolean(file.isDirectory());
                }
            } else {
                output.writeInt(0);
            }

            content = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (content == null) {
            throw new InternalError();
        }

        return new SmallResponse(content);
    }
}