package ru.spbau.shavkunov.vcs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * Класс, осуществляющий всю внутреннюю работу с репозиторием.
 */
public class Repository {
    private static final String OBJECTS_FOLDER = "objects";
    private static final String REFERENCES_FOLDER = "references";
    private static final String VCS_FOLDER = ".vsc";

    private Path rootDirectory;

    /**
     * Инициализация репозитория.
     * @param path путь к папке, где будет создан репозиторий.
     * @throws IOException исключения, могут возникнуть в случае если невозможно создать папку
     * или переданный путь это не папка.
     */
    public static void initRepository(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }

        Path rootDir = path.resolve(VCS_FOLDER);

        Files.createDirectory(rootDir);
        Files.createDirectory(rootDir.resolve(OBJECTS_FOLDER));
        Files.createDirectory(rootDir.resolve(REFERENCES_FOLDER));
    }

    public static Repository getRepository(Path path) throws IOException, NoRepositoryException {
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }

        Path rootDir = path.resolve(VCS_FOLDER);
        if (!Files.exists(rootDir)) {
            throw new NoRepositoryException();
        }

        return new Repository(path);
    }

    public Repository(Path rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory;
    }
}
