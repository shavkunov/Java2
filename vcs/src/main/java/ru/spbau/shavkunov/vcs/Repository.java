package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRepositoryException;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ru.spbau.shavkunov.vcs.Constants.REFERENCE_PREFIX;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;

/**
 * Класс, осуществляющий всю внутреннюю работу с репозиторием.
 */
public class Repository {
    /**
     * Логгер этого класса.
     */
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(Repository.class);

    private @NotNull Datastore data;

    /**
     * Представление файла индекса -- множество путей файлов и их хешей.
     */
    private @NotNull Map<Path, String> index;

    public void initResources() throws IOException {
        data.initResources();
    }

    /**
     * Добавить файл в index.
     * @param pathToFile путь к файлу.
     * @param hash хеш добавляемого файла.
     */
    public void addFileToIndex(@NotNull Path pathToFile, @NotNull String hash) throws IOException {
        logger.debug("Adding file " + pathToFile + " to index");
        index.put(pathToFile, hash);
        data.updateIndex(index);
    }

    /**
     * Удалить файл из index.
     * @param pathToFile путь к файлу.
     */
    public void removeFileFromIndex(@NotNull Path pathToFile) throws IOException, NotRegularFileException {
        logger.debug("Adding file " + pathToFile + " from index");

        pathToFile = pathToFile.normalize();
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        pathToFile.toFile().delete();
        index.remove(pathToFile);
        data.updateIndex(index);
    }

    /**
     * Создание дерева структуры файлов и папок репозитория.
     * @return дерево с структурой папок.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws NotRegularFileException исключение, если вдруг объект Blob создается не от файла.
     */
    public @NotNull VcsTree createTreeFromIndex() throws IOException, NotRegularFileException {
        logger.debug("Start creating tree from index");
        TreeMap<Path, VcsTree> trees = new TreeMap<>();
        Path rootPath = Paths.get(".").normalize();
        trees.put(rootPath, new VcsTree(rootPath));
        for (Path pathToFile : index.keySet()) {
            Path absolutePrefix = rootPath;
            for (Path prefix : pathToFile) {
                logger.debug(prefix.toString());
                absolutePrefix = absolutePrefix.resolve(prefix);
                VcsTree selectedVcsTree;
                if (absolutePrefix.equals(pathToFile)) {
                    Blob blob = new Blob(pathToFile);

                    if (pathToFile.getParent() == null) {
                        selectedVcsTree = trees.get(rootPath);
                    } else {
                        selectedVcsTree = trees.get(pathToFile.getParent());
                    }

                    selectedVcsTree.addBlob(blob, pathToFile.toString());
                } else {
                    if (!trees.containsKey(absolutePrefix)) {
                        VcsTree prefixVcsTree = new VcsTree(prefix);
                        trees.put(absolutePrefix, prefixVcsTree);
                        if (absolutePrefix.getParent() != null) {
                            selectedVcsTree = trees.get(absolutePrefix.getParent());
                        } else {
                            selectedVcsTree = trees.get(rootPath);
                        }

                        selectedVcsTree.addChild(prefixVcsTree);
                    }
                }
            }
        }

        logger.debug("Computing tree hashes:");
        ArrayList<Path> pathsInTree = new ArrayList<>(trees.keySet());
        Collections.reverse(pathsInTree);
        for (Path path : pathsInTree) {
            trees.get(path).computeHash(data);
            logger.debug(trees.get(path).getHash());
        }
        VcsTree resVcsTree = trees.get(rootPath);

        logger.debug("Tree with hash " + resVcsTree.getHash() + " from index was created");
        return resVcsTree;
    }

    /**
     * Получение содержимого файла head.
     * @return содержимое файла head.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public @NotNull String getCurrentHead() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(data.getHead().toFile()));

        return reader.readLine();
    }

    /**
     * Запись информации в файл head.
     * @param revision имя ветки или хеш коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void writeHead(@NotNull String revision) throws IOException {
        if (data.getReferencesPath().resolve(revision).toFile().exists()) {
            Files.write(data.getHead(), (REFERENCE_PREFIX + revision).getBytes());
        } else {
            Files.write(data.getHead(), revision.getBytes());
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

        return new Repository(path);
    }

    public Repository(@NotNull Path rootDirectory) throws IOException {
        data = new Filesystem(rootDirectory);
        index = data.readIndex();
    }

    /**
     * Удаление ветки.
     * @param branchName ветку с этим именем требуется удалить.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void deleteBranch(@NotNull String branchName) throws IOException {
        Files.delete(data.getReferencesPath().resolve(branchName));
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

        Path branchPath = data.getReferencesPath().resolve(branchName);
        Files.write(branchPath, commitHash.getBytes());
        Files.write(data.getHead(), (REFERENCE_PREFIX + branchName).getBytes());
    }

    /**
     * Проверка существования ветки.
     * @param branchName имя проверяемой ветки.
     * @return true, если ветка с данным именем существует, false иначе.
     */
    public boolean isBranchExists(@NotNull String branchName) {
        Path branchPath = data.getReferencesPath().resolve(branchName);
        return branchPath.toFile().exists();
    }

    /**
     * Проверка существования коммита.
     * @param commitHash хеш коммита.
     * @return true, если коммит с таким хешом существует, иначе false.
     */
    public boolean isCommitExists(@NotNull String commitHash) {
        return data.getObjectsPath().resolve(commitHash).toFile().exists();
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

    /**
     * Восстановление содержимого файла до состояния коммита.
     * @param pathToFile путь к файлу.
     * @param fileHash хеш файла.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash) throws IOException {
        data.restoreFile(pathToFile, fileHash);
    }

    /**
     * Добавление файла в репозиторий.
     * @param blob добавляемый файл.
     * @throws IOException
     */
    public void storeFile(@NotNull Blob blob) throws IOException {
        addFileToIndex(blob.getPathToFile(), blob.getHash());
        data.storeObject(blob);
    }

    public void restoreTree(@NotNull VcsTree tree) throws IOException {
        index = new HashMap<>();
        data.addTree(tree, data.getRootDirectory());
        data.updateIndex(index);
    }

    /**
     * Создание файла index из дерева.
     * @param vcsTree дерево файлов.
     */
    public void createIndexFromTree(@NotNull VcsTree vcsTree) {
        logger.debug("Creating index from tree " + vcsTree.getHash());
        index.clear();
        HashSet<ObjectWithName<Blob>> files = vcsTree.getAllFiles();
        for (ObjectWithName<Blob> file : files) {
            Blob blob = file.getContent();
            String filePath = file.getName();
            String fileHash = blob.getHash();
            index.put(Paths.get(filePath), fileHash);
        }
    }

    public FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) {
        return data.getFilesTree(excludeFiles);
    }

    public void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException,
                                                                NoRootDirectoryExistsException, IOException {
        data.clean(untrackedFiles);
    }
}