package ru.spbau.shavkunov.vcs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import static ru.spbau.shavkunov.vcs.Constants.*;

/**
 * Класс, осуществляющий всю внутреннюю работу с репозиторием.
 */
public class Repository {
    private Path rootDirectory;

    public Path getRootDirectory() {
        return rootDirectory;
    }

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
        Files.createFile(rootDir.resolve(INDEX_FILE));

        Path pathToHead = rootDir.resolve(HEAD);
        Files.createFile(pathToHead);
        FileOutputStream fileOutputStream = new FileOutputStream(pathToHead.toFile());
        fileOutputStream.write(DEFAULT_BRANCH_NAME.getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public Path getIndexPath() {
        return rootDirectory.resolve(INDEX_FILE);
    }

    public Path getObjectsPath() {
        return rootDirectory.resolve(OBJECTS_FOLDER);
    }

    public Path getCurrentBranchName() {
        return rootDirectory.resolve(REFERENCES_FOLDER);
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
