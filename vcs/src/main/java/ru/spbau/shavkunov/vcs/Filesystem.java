package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ru.spbau.shavkunov.vcs.Constants.*;
import static ru.spbau.shavkunov.vcs.Constants.DEFAULT_BRANCH_NAME;

public class Filesystem implements Datastore {
    /**
     * Логгер этого класса.
     */
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(Filesystem.class);

    private @NotNull Path rootDirectory;

    @Override
    public void initResources() throws IOException {
        if (!Files.isDirectory(rootDirectory)) {
            throw new NotDirectoryException(rootDirectory.toString());
        }

        Path rootDir = rootDirectory.resolve(VCS_FOLDER).normalize();

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

    @Override
    public void createNewBranch(@NotNull String branchName, @NotNull String commitHash)
                                            throws BranchAlreadyExistsException, IOException {
        if (isBranchExists(branchName)) {
            throw new BranchAlreadyExistsException();
        }

        Path branchPath = getReferencesPath().resolve(branchName);
        Files.write(branchPath, commitHash.getBytes());
        Files.write(getHeadPath(), (REFERENCE_PREFIX + branchName).getBytes());
    }

    @Override
    public void deleteBranch(@NotNull String branchName) throws IOException {
        Files.delete(getReferencesPath().resolve(branchName));
    }

    @Override
    public boolean isBranchExists(@NotNull String branchName) {
        Path branchPath = getReferencesPath().resolve(branchName);
        return branchPath.toFile().exists();
    }

    @Override
    public boolean isCommitExists(@NotNull String commitHash) {
        return getObjectsPath().resolve(commitHash).toFile().exists();
    }

    @NotNull
    @Override
    public Path getRootDirectory() {
        return rootDirectory;
    }
    
    public Filesystem(@NotNull Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void writeHead(@NotNull String revision) throws IOException {
        if (getReferencesPath().resolve(revision).toFile().exists()) {
            Files.write(getHeadPath(), (REFERENCE_PREFIX + revision).getBytes());
        } else {
            Files.write(getHeadPath(), revision.getBytes());
        }
    }

    @Override
    public void writeContent(@NotNull Path pathToFile, @NotNull byte[] content) {

    }

    @Override
    public void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash) {

    }

    @Override
    public void storeObject(@NotNull VcsObjectWithHash object) {

    }

    @Override
    public void storeReference(@NotNull String name, @NotNull String commitHash) throws IOException {
        Path pathToRef = getReferencesPath().resolve(name);
        if (!pathToRef.toFile().exists()) {
            pathToRef.toFile().createNewFile();
        }

        Files.write(pathToRef, commitHash.getBytes());
    }

    /**
     * Получение коммита по хешу.
     * @param commitHash хеш коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws ClassNotFoundException исключение, если не удалось интерпретировать данные(хеш не коммита)
     */
    @NotNull
    @Override
    public Commit getCommitByHash(@NotNull String commitHash) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(getObjectsPath().resolve(commitHash));

        return new Commit(content, commitHash);
    }

    @Override
    public @NotNull VcsTree getTreeByHash(@NotNull String treeHash) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(getObjectsPath().resolve(treeHash));

        return new VcsTree(treeHash, content);
    }

    @NotNull
    @Override
    public String getReferenceCommitHash(@NotNull String referenceName) throws IOException {
        return getFirstLine(getReferencesPath().resolve(referenceName));
    }

    /**
     * Получение первой строчки файла.
     * @param pathToFile путь к файлу.
     * @return первая строчка данного файла.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public static String getFirstLine(@NotNull Path pathToFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));
        String line = reader.readLine();
        if (line == null) {
            line = "";
        }

        return line;
    }

    @NotNull
    @Override
    public FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) {
        return null;
    }

    /**
     * Получение ссылки на файл index.
     * @return путь к файлу index.
     */
    private @NotNull Path getIndexPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(INDEX_FILE);
    }

    /**
     * Получение ссылки на папку, где хранятся все объекты.
     * @return путь к папке объектов.
     */
    private @NotNull Path getObjectsPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(OBJECTS_FOLDER);
    }

    /**
     * Получение ссылки на папку, где хранятся все ветки.
     * @return путь к папке ссылок.
     */
    private @NotNull Path getReferencesPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(REFERENCES_FOLDER);
    }

    /**
     * Получение ссылки на head файл.
     * @return путь к файлу head.
     */
    @Override
    public @NotNull BufferedReader getHead() throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(getHeadPath().toFile()));

        return reader;
    }

    private @NotNull Path getHeadPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(HEAD);
    }

    /**
     * Чтение файла index, отвечающий за состояние репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    @Override
    public @NotNull Map<Path, String> readIndex() throws IOException {
        logger.debug("Reading index file");

        Map<Path, String> index = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(getIndexPath().toFile()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) {
                continue;
            }
            String[] pathToFileWithHash = line.split(" ");
            Path pathToFile = Paths.get(pathToFileWithHash[0]);
            String hash = pathToFileWithHash[1];
            index.put(pathToFile, hash);
        }

        logger.debug("Reading index is complete");

        return index;
    }


    /**
     * Добавление в корневую папку дополнительную структуру файлов.
     * @param vcsTree в этом объекте хранится вся структура файлов и папок.
     * @param root корневой путь, куда нужно добавить дерево
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    @Override
    public void addTree(@NotNull VcsTree vcsTree, @NotNull Path root) throws IOException {
        logger.debug("Adding tree with hash " + vcsTree.getHash() + " to " + root);
        for (ObjectWithName<Blob> file : vcsTree.getBlobFiles()) {
            Blob blob = file.getContent();
            Path fileName = Paths.get(file.getName());

            fileName.toFile().createNewFile();
            byte[] content = Files.readAllBytes(getObjectsPath().resolve(blob.getHash()));
            writeContent(fileName, content);
        }

        for (VcsTree subVcsTree : vcsTree.getVcsTreeFiles()) {
            Path treeDirectory = root.resolve(subVcsTree.getPrefix());
            if (!treeDirectory.toFile().exists()) {
                Files.createDirectory(treeDirectory);
            }
            addTree(subVcsTree, treeDirectory);
        }
    }

    /**
     * Сохранение изменений в файл index.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    @Override
    public void updateIndex(@NotNull Map<Path, String> index) throws IOException {
        logger.debug("Updating index file");

        FileWriter fileWriter = new FileWriter(getIndexPath().toFile());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        for (Map.Entry<Path, String> entry : index.entrySet()) {
            String line = entry.getKey().toString() + " " + entry.getValue();
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();

        logger.debug("Updating index is complete");
    }

    /**
     * Реализация команды clean. Удаление всех файлов, не находящихся под контролем системы версий.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException,
            NoRootDirectoryExistsException, IOException {
        logger.debug("Cleaning repository");

        for (String path : untrackedFiles) {
            File file = rootDirectory.resolve(path).toFile();
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }
}