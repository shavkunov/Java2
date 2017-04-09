package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
    public Path getRootDirectory() {
        return rootDirectory;
    }
    
    public Filesystem(@NotNull Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void writeContent(Path pathToFile, byte[] content) {

    }

    @Override
    public void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash) {

    }

    @Override
    public void storeObject(VcsObjectWithHash object) {

    }

    @Override
    public FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) {
        return null;
    }

    /**
     * Получение ссылки на файл index.
     * @return путь к файлу index.
     */
    @Override
    public @NotNull Path getIndexPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(INDEX_FILE);
    }

    /**
     * Получение ссылки на папку, где хранятся все объекты.
     * @return путь к папке объектов.
     */
    @Override
    public @NotNull Path getObjectsPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(OBJECTS_FOLDER);
    }

    /**
     * Получение ссылки на папку, где хранятся все ветки.
     * @return путь к папке ссылок.
     */
    @Override
    public @NotNull Path getReferencesPath() {
        return rootDirectory.resolve(VCS_FOLDER).resolve(REFERENCES_FOLDER);
    }

    /**
     * Получение ссылки на head файл.
     * @return путь к файлу head.
     */
    @Override
    public @NotNull Path getHead() {
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