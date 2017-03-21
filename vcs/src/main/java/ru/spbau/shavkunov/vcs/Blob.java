package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Blob extends VcsObjectWithHash {
    /**
     * Путь к файлу пользователя.
     */
    public Blob(Path path, Repository repository) throws NotRegularFileException, IOException {
        if (Files.isDirectory(path)) {
            throw new NotRegularFileException();
        }

        hash = DigestUtils.sha1Hex(Files.readAllBytes(path));
        if (Files.exists(repository.getObjectsPath().resolve(hash))) {
            Files.copy(path, repository.getObjectsPath().resolve(hash), NOFOLLOW_LINKS);
        }
    }

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}