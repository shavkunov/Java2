package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Blob extends VcsObject {
    private Path path;

    public Blob(Path path) throws NotRegularFileException, IOException {
        if (Files.isDirectory(path)) {
            throw new NotRegularFileException();
        }

        this.path = path;
        hash = DigestUtils.sha1Hex(Files.readAllBytes(path));
    }

    public Path getPath() {
        return path;
    }
}
