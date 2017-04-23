package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;
import ru.spbau.shavkunov.vcs.exceptions.RepositoryAlreadyExistsException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

/**
 * Интерфейс, позволяющий репозиторию работать с данными.
 */
public interface Datastore {
    /**
     * {@link Repository#writeHead(String)}.
     */
    void writeHead(@NotNull String revision) throws IOException;

    /**
     * Заполнить файл контентом.
     * @param pathToFile путь к файлу.
     * @param content информация, которая будет содержаться в файле.
     * @throws IOException исключение, если возникли проблемы с вводом/выводом.
     */
    void writeContent(@NotNull Path pathToFile, @NotNull byte[] content) throws IOException;

    /**
     * {@link Repository#restoreFile(Path, String)}
     */
    void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash) throws IOException;

    /**
     * {@link Repository#storeObject(VcsObjectWithHash)}
     */
    void storeObject(@NotNull VcsObjectWithHash object) throws IOException;

    /**
     * {@link Repository#getCurrentHead()}
     */
    @NotNull String getHead() throws IOException;

    /**
     * {@link Repository#storeReferenceCommit(String, String)}
     */
    void storeReference(@NotNull String name, @NotNull String commitHash) throws IOException;

    /**
     * Получение коммита по его хешу.
     * @param commitHash хеш коммита.
     * @return объект Commit, хеш которого находится внутри VCS.
     * @throws IOException исключение, если возникли проблемы с вводом/выводом.
     * @throws ClassNotFoundException исключение происходит при неправильном касте объектов.
     */
    @NotNull Commit getCommitByHash(@NotNull String commitHash) throws IOException, ClassNotFoundException;

    /**
     * Получение дерева по его хешу.
     * @param treeHash хеш дерева
     * @return объект Tree, хеш которого находится в VCS.
     * @throws IOException исключение, если возникли проблемы с вводом/выводом.
     * @throws ClassNotFoundException исключение происходит при неправильном касте объектов.
     */
    @NotNull VcsTree getTreeByHash(@NotNull String treeHash) throws IOException, ClassNotFoundException;

    /**
     * Получение коммита, на который указывает ссылка.
     * @param referenceName имя ссылки, коммит которой нужно получить.
     * @return хеш коммита
     * @throws IOException исключение, если возникли проблемы с вводом/выводом.
     */
    @NotNull String getReferenceCommitHash(@NotNull String referenceName) throws IOException;

    /**
     * Получение дерева файлов, исключая данные.
     * @param excludeFiles файлы, которые не должны входить в дерево.
     * @return объект FilesTree
     * @throws NoRootDirectoryExistsException исключение, если не существует корневой директории.
     */
    @NotNull FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) throws NoRootDirectoryExistsException;

    /**
     * Инициализация ресурсов в заданной директории.
     * @param rootDirectory директория, где будет хранится репозиторий.
     * @throws IOException исключение, если возникли проблемы с вводом/выводом.
     */
    void initResources(@NotNull Path rootDirectory) throws IOException, RepositoryAlreadyExistsException;

    /**
     * {@link Repository#createNewBranch(String, String)}
     */
    void createNewBranch(@NotNull String branchName, @NotNull String commitHash)
                                                        throws BranchAlreadyExistsException, IOException;

    /**
     * {@link Repository#deleteBranch(String)}
     */
    void deleteBranch(@NotNull String branchName) throws IOException;

    /**
     * {@link Repository#isBranchExists(String)}
     */
    boolean isBranchExists(@NotNull String branchName);

    /**
     * {@link Repository#isCommitExists(String)}
     */
    boolean isCommitExists(@NotNull String commitHash);

    /**
     * Получение корневой папки, где расположен репозиторий.
     * @return путь к папке репозитория.
     */
    @NotNull Path getRootDirectory();

    /**
     * {@link Filesystem#updateIndex(Map)}
     */
    void updateIndex(@NotNull Map<Path, String> index) throws IOException;

    /**
     * {@link Filesystem#readIndex()}
     */
    @NotNull Map<Path, String> readIndex() throws IOException;

    /**
     * {@link Filesystem#addTree(VcsTree, Path)}
     */
    void addTree(@NotNull VcsTree tree, @NotNull Path rootPath) throws IOException;

    /**
     * {@link Filesystem#clean(HashSet)}
     */
    void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException,
                                                               NoRootDirectoryExistsException, IOException;
}