package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

/**
 * Класс, представляющий собой обертку над файлом пользователя.
 */
public class Blob extends VcsObjectWithHash {
    /**
     * Создание объекта Blob.
     * @param path путь к файлу пользователя.
     * @param repository репозиторий, куда добавится этот blob.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Blob(Path path, Repository repository) throws NotRegularFileException, IOException {
        if (Files.isDirectory(path)) {
            throw new NotRegularFileException();
        }

        hash = DigestUtils.sha1Hex(Files.readAllBytes(path));
        Files.copy(path, repository.getObjectsPath().resolve(hash), REPLACE_EXISTING, NOFOLLOW_LINKS);
    }

    /**
     * Заполнение файла контентом объекта Blob
     * @param pathToFile путь к файлу
     * @param repository репозиторий, где лежит объект Blob
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void fillFileWithContent(Path pathToFile, Repository repository) throws IOException {
        byte[] content = Files.readAllBytes(getPathToObject(repository));
        Files.write(pathToFile, content);
    }

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}