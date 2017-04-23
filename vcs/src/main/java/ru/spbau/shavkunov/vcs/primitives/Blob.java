package ru.spbau.shavkunov.vcs.primitives;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс, представляющий собой обертку над файлом пользователя.
 */
public class Blob extends VcsObjectWithHash {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(Blob.class);

    /**
     * Путь к файлу пользователя.
     */
    private @NotNull String pathToFile;

    /**
     * Получение пути к файлу.
     * @return возвращает путь к файлу.
     */
    public @NotNull Path getPathToFile() {
        return Paths.get(pathToFile);
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

        pathToFile = path.normalize().toString();
        hash = DigestUtils.sha1Hex(Files.readAllBytes(path));
        logger.debug("Created blob with hash : " + hash);
    }

    @Override
    public @NotNull byte[] getContent() throws IOException {
        return Files.readAllBytes(getPathToFile());
    }
}