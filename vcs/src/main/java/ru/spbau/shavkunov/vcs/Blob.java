package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Blob extends VcsObjectWithHash {
    /**
     * Путь к файлу пользователя.
     */
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

    @Override
    public void saveToStorage(Repository repository) throws IOException {
        Files.copy(path, repository.getObjectsPath().resolve(hash), REPLACE_EXISTING, NOFOLLOW_LINKS);
    }

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}