package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
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
    /**
     * Корневая директория, где расположен репозиторий.
     */
    private @NotNull Path rootDirectory;

    public @NotNull Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Инициализация репозитория.
     * @param path путь к папке, где будет создан репозиторий.
     * @throws IOException исключения, могут возникнуть в случае если невозможно создать папку
     * или переданный путь это не папка.
     */
    public static void initRepository(@NotNull Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }

        Path rootDir = path.resolve(VCS_FOLDER).normalize();

        Files.createDirectory(rootDir);
        Files.createDirectory(rootDir.resolve(OBJECTS_FOLDER));
        Files.createDirectory(rootDir.resolve(REFERENCES_FOLDER));
        Files.createFile(rootDir.resolve(REFERENCES_FOLDER).resolve(DEFAULT_BRANCH_NAME));
        Files.createFile(rootDir.resolve(INDEX_FILE));

        Path pathToHead = rootDir.resolve(HEAD);
        Files.createFile(pathToHead);
        FileOutputStream fileOutputStream = new FileOutputStream(pathToHead.toFile());
        fileOutputStream.write((REFERENCE_PREFIX + DEFAULT_BRANCH_NAME).getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * Получение ссылки на файл index.
     * @return путь к файлу index.
     */
    public @NotNull Path getIndexPath() {
        return rootDirectory.resolve(INDEX_FILE);
    }

    /**
     * Получение ссылки на папку, где хранятся все объекты.
     * @return путь к папке объектов.
     */
    public @NotNull Path getObjectsPath() {
        return rootDirectory.resolve(OBJECTS_FOLDER);
    }

    /**
     * Получение ссылки на папку, где хранятся все ветки.
     * @return путь к папке ссылок.
     */
    public @NotNull Path getReferencesPath() {
        return rootDirectory.resolve(REFERENCES_FOLDER);
    }

    /**
     * Получение ссылки на head файл.
     * @return путь к файлу head.
     */
    private @NotNull Path getHead() {
        return rootDirectory.resolve(HEAD);
    }

    /**
     * Получение содержимого файла head.
     * @return содержимое файла head.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public @NotNull String getCurrentHead() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(getHead().toFile()));

        return reader.readLine();
    }

    /**
     * Запись информации в файл head.
     * @param revision имя ветки или хеш коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void writeHead(@NotNull String revision) throws IOException {
        if (getReferencesPath().resolve(revision).toFile().exists()) {
            Files.write(getHead(), (REFERENCE_PREFIX + revision).getBytes());
        } else {
            Files.write(getHead(), revision.getBytes());
        }
    }

    /**
     * Создание репозитория.
     * @param path путь к репозиторию
     * @return Инстанс класса Repository
     * @throws IOException исключение, если возникли проблемы с чтением файла или файл не оказался директорией.
     * @throws NoRepositoryException исключение, если по данному пути нет репозитория.
     */
    public static Repository getRepository(@NotNull Path path) throws IOException, NoRepositoryException {
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }

        Path rootDir = path.resolve(VCS_FOLDER);
        if (!Files.exists(rootDir)) {
            throw new NoRepositoryException();
        }

        return new Repository(rootDir);
    }

    public Repository(@NotNull Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /**
     * Удаление ветки.
     * @param branchName ветку с этим именем требуется удалить.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void deleteBranch(@NotNull String branchName) throws IOException {
        Files.delete(getReferencesPath().resolve(branchName));
    }

    /**
     * Создание новой ветки.
     * @param branchName имя новой ветки.
     * @param commitHash хеш первого коммита этой ветки.
     * @throws BranchAlreadyExistsException ветка на самом деле уже была создана.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void createNewBranch(@NotNull String branchName, @NotNull String commitHash)
                                throws BranchAlreadyExistsException, IOException {
        if (isBranchExists(branchName)) {
            throw new BranchAlreadyExistsException();
        }

        Path branchPath = getReferencesPath().resolve(branchName);
        Files.write(branchPath, commitHash.getBytes());
        Files.write(getHead(), (REFERENCE_PREFIX + branchName).getBytes());
    }

    /**
     * Проверка существования ветки.
     * @param branchName имя проверяемой ветки.
     * @return true, если ветка с данным именем существует, false иначе.
     */
    public boolean isBranchExists(@NotNull String branchName) {
        Path branchPath = getReferencesPath().resolve(branchName);
        return branchPath.toFile().exists();
    }

    public boolean isCommitExists(@NotNull String commitHash) {
        return getObjectsPath().resolve(commitHash).toFile().exists();
    }

    public static String getFirstLine(Path pathToFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));
        String line = reader.readLine();
        if (line == null) {
            line = "";
        }

        return line;
    }

    public void restoreFile(Path pathToFile, String fileHash) throws IOException {
        Files.write(pathToFile, Files.readAllBytes(getObjectsPath().resolve(fileHash)));
    }
}