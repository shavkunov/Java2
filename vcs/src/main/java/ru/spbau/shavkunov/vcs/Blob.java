package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Класс, представляющий собой обертку над файлом пользователя.
 */
public class Blob extends VcsObjectWithHash {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(Blob.class);

    private Path pathToFile;

    public Path getPathToFile() {
        return pathToFile;
    }

    /**
     * Создание объекта Blob.
     * @param path путь к файлу пользователя.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Blob(@NotNull Path path) throws NotRegularFileException, IOException {
        if (Files.isDirectory(path)) {
            throw new NotRegularFileException();
        }

        pathToFile = path.normalize();
        hash = DigestUtils.sha1Hex(Files.readAllBytes(path));
        logger.debug("Created blob with hash : " + hash);
    }

    @Override
    public byte[] getContent() throws IOException {
        return Files.readAllBytes(pathToFile);
    }
}