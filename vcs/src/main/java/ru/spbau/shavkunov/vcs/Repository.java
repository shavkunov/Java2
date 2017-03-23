package ru.spbau.shavkunov.vcs;

import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRepositoryException;

import java.io.*;
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
        fileOutputStream.write((REFERENCE_PREFIX + DEFAULT_BRANCH_NAME).getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public Path getIndexPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(INDEX_FILE);
    }

    public Path getObjectsPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(OBJECTS_FOLDER);
    }

    public Path getReferencesPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(REFERENCES_FOLDER);
    }

    private Path getHead() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(HEAD);
    }

    public String getCurrentHead() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(getHead().toFile()));
        return reader.readLine();
    }

    public void writeHead(String revision) throws IOException {
        if (getReferencesPath().resolve(revision).toFile().exists()) {
            Files.write(getHead(), (REFERENCE_PREFIX + revision).getBytes());
        } else {
            Files.write(getHead(), revision.getBytes());
        }
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

    public void deleteBranch(String branchName) throws IOException {
        Files.delete(getReferencesPath().resolve(branchName));
    }

    public void createNewBranch(String branchName, String commitHash) throws BranchAlreadyExistsException, IOException {
        if (isBranchExists(branchName)) {
            throw new BranchAlreadyExistsException();
        }

        Path branchPath = getReferencesPath().resolve(branchName);
        Files.write(branchPath, commitHash.getBytes());
        Files.write(getHead(), (REFERENCE_PREFIX + branchName).getBytes());
    }

    public boolean isBranchExists(String branchName) {
        Path branchPath = getReferencesPath().resolve(branchName);
        return branchPath.toFile().exists();
    }
}
