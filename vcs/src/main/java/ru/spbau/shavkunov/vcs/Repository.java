package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Класс, осуществляющий всю внутреннюю работу с репозиторием.
 */
public class Repository {
    /**
     * Логгер этого класса.
     */
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(Repository.class);

    /**
     * Инстанс класса для работы с данными.
     */
    private @NotNull Datastore data;

    /**
     * Представление файла индекса -- множество путей файлов и их хешей.
     */
    private @NotNull Map<Path, String> index;

    /**
     * Создание репозитория с файловым представляем VCS.
     * @param fileSystem реализация хранения информации на файлах.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Repository(@NotNull Filesystem fileSystem) throws IOException {
        data = fileSystem;
        index = data.readIndex();
    }

    /**
     * Инициализация ресурсов репозитория.
     * @param pathToRepo папка, где будет располагаться репозиторий.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public static void initResources(@NotNull Path pathToRepo) throws IOException {
        new Filesystem().initResources(pathToRepo);
    }

    /**
     * Добавить файл в index.
     * @param pathToFile путь к файлу.
     * @param hash хеш добавляемого файла.
     */
    public void addFileToIndex(@NotNull Path pathToFile, @NotNull String hash) throws IOException {
        logger.debug("Adding file {} to index", pathToFile);
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
            trees.get(path).computeHash();
            storeObject(trees.get(path));
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
        return data.getHead();
    }

    /**
     * Запись информации в файл head.
     * @param revision имя ветки или хеш коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void writeHead(@NotNull String revision) throws IOException {
        data.writeHead(revision);
    }

    /**
     * Создание репозитория.
     * @param path путь к репозиторию
     * @throws IOException исключение, если возникли проблемы с чтением файла или файл не оказался директорией.
     * @throws NoRepositoryException исключение, если по данному пути нет репозитория.
     */
    public Repository(@NotNull Path path) throws IOException, NoRepositoryException {
        data = new Filesystem(path);
        index = data.readIndex();
    }

    /**
     * Удаление ветки.
     * @param branchName ветку с этим именем требуется удалить.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void deleteBranch(@NotNull String branchName) throws IOException, NoBranchExistsException, CannotDeleteCurrentBranchException {
        if (isBranchExists(branchName)) {
            String currentHead = getCurrentHead();
            if (currentHead.equals(branchName)) {
                logger.error("Tried to delete current branch");
                throw new CannotDeleteCurrentBranchException();
            }

            data.deleteBranch(branchName);
            logger.debug("Deleted branch " + branchName);
        } else {
            logger.error("Branch with name " + branchName + " doesn't exist");
            throw new NoBranchExistsException();
        }
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

        data.createNewBranch(branchName, commitHash);
    }

    /**
     * Проверка существования ветки.
     * @param branchName имя проверяемой ветки.
     * @return true, если ветка с данным именем существует, false иначе.
     */
    public boolean isBranchExists(@NotNull String branchName) {
        return data.isBranchExists(branchName);
    }

    /**
     * Проверка существования коммита.
     * @param commitHash хеш коммита.
     * @return true, если коммит с таким хешом существует, иначе false.
     */
    public boolean isCommitExists(@NotNull String commitHash) {
        return data.isCommitExists(commitHash);
    }

    /**
     * Восстановление содержимого файла до состояния коммита.
     * @param pathToFile путь к файлу.
     * @param fileHash хеш файла в системе контроля версий.
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

    /**
     * Сохранение объекта в системе контроля версий.
     * @param object объект, который нужно сохранить.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void storeObject(@NotNull VcsObjectWithHash object) throws IOException {
        data.storeObject(object);
    }

    /**
     * Обновление коммита, на который указывает ссылка.
     * @param name имя ссылки, которой нужно обновить хеш коммита.
     * @param commitHash хеш коммита, на который будет указывать ссылка.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void storeReferenceCommit(@NotNull String name, @NotNull String commitHash) throws IOException {
        data.storeReference(name, commitHash);
    }

    /**
     * Получение коммита по его хешу.
     * @param hash хеш коммита.
     * @return инстанс класса Commit с хешом из VCS.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws ClassNotFoundException исключение, если не удалось интерпретировать данные(хеш не коммита).
     */
    public @NotNull Commit getCommit(@NotNull String hash) throws IOException, ClassNotFoundException {
        return data.getCommitByHash(hash);
    }

    /**
     * Получение дерева по его хешу.
     * @param hash хеш дерева
     * @return дерево файлов из VCS.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws ClassNotFoundException исключение, если не удалось интерпретировать данные(хеш не дерева).
     */
    public @NotNull VcsTree getTree(@NotNull String hash) throws IOException, ClassNotFoundException {
        return data.getTreeByHash(hash);
    }

    /**
     * Получение коммита, на который указывает ссылка.
     * @param referenceName имя ссылки.
     * @return хеш коммита, который содержится в ссылке.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public @NotNull String getReferenceCommitHash(@NotNull String referenceName) throws IOException {
        return data.getReferenceCommitHash(referenceName);
    }

    /**
     * Восстановление файлов до состояния дерева коммита.
     * @param tree дерево файлов коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public void restoreTree(@NotNull VcsTree tree) throws IOException {
        index = new HashMap<>();
        data.addTree(tree, data.getRootDirectory());
        data.updateIndex(index);
    }

    /**
     * {@link Datastore#getRootDirectory()}
     */
    public @NotNull Path getRootDirectory() {
        return data.getRootDirectory();
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

    /**
     * Создание дерева файлов.
     * @param excludeFiles файлы, которые не должны входить в дерево файлов.
     * @throws NoRootDirectoryExistsException исключение, если не существует корневой директории.
     */
    public @NotNull FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) throws NoRootDirectoryExistsException {
        return data.getFilesTree(excludeFiles);
    }

    /**
     * Реализация команды clean. Удаление всех файлов, не находящихся под контролем системы версий.
     * @param untrackedFiles файлы, которые не находятся под контролем.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException,
                                                                NoRootDirectoryExistsException, IOException {
        data.clean(untrackedFiles);
    }

    /**
     * Получение текущей ссылки репозитория.
     * @return объект текущей ссылки
     * @throws IOException исключение, если возникли проблемы с файлом.
     */
    public Reference getReference() throws IOException {
        String name = getCurrentHead();
        String commitHash;

        if (isBranchExists(name)) {
            commitHash = getReferenceCommitHash(name);
        } else {
            commitHash = name;
            name = "Commit hash";
        }

        return new Reference(name, commitHash);
    }
}