package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.vcs.exceptions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ru.spbau.shavkunov.vcs.Constants.MERGE_MESSAGE;
import static ru.spbau.shavkunov.vcs.Constants.REFERENCE_PREFIX;
import static ru.spbau.shavkunov.vcs.Constants.USERNAME;

/**
 * Класс отвечающий за логику взаимодействия между пользователем и репозиторием.
 */
public class VcsManager {
    /**
     * Репозитоий, который упрявляется менеджером.
     */
    private @NotNull Repository repository;

    /**
     * Представление файла индекса -- множество путей файлов и их хешей.
     */
    private @NotNull Map<Path, String> index;

    /**
     * Чтение файла index, отвечающий за состояние репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void readIndex() throws IOException {
        index = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(repository.getIndexPath().toFile()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] pathToFileWithHash = line.split(" ");
            Path pathToFile = Paths.get(pathToFileWithHash[0]);
            String hash = pathToFileWithHash[1];
            index.put(pathToFile, hash);
        }
    }

    /**
     * Сохранение изменений в файл index.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void updateIndex() throws IOException {
        FileWriter fileWriter = new FileWriter(repository.getIndexPath().toFile());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        for (Map.Entry<Path, String> entry : index.entrySet()) {
            String line = entry.getKey().toString() + " " + entry.getValue();
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public VcsManager(@NotNull Repository repository) throws IOException {
        this.repository = repository;
        readIndex();
    }

    public VcsManager(@NotNull Path pathToRepo) throws IOException {
        this.repository = new Repository(pathToRepo);
        readIndex();
    }

    public Map<Path, String> getIndex() {
        return index;
    }

    /**
     * Добавить файл в index.
     * @param pathToFile путь к файлу.
     * @param hash хеш добавляемого файла.
     */
    private void addFileToIndex(@NotNull Path pathToFile, @NotNull String hash) throws IOException {
        index.put(pathToFile, hash);
        updateIndex();
    }

    /**
     * Удалить файл из index.
     * @param pathToFile путь к файлу.
     */
    private void removeFileFromIndex(@NotNull Path pathToFile) throws IOException {
        index.remove(pathToFile);
        updateIndex();
    }

    /**
     * Реализация команды remove. Удаление файла из репозитория.
     * @param pathToFile путь к удаляемому файлу.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     */
    public void removeFile(@NotNull Path pathToFile) throws NotRegularFileException, IOException {
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        removeFileFromIndex(pathToFile.normalize());
    }

    /**
     * Реализация команды add репозитория.
     * @param pathToFile путь к файлу, который нужно добавить.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @return хеш файла, добавленного в репозиторий.
     */
    public String addFile(@NotNull Path pathToFile) throws NotRegularFileException, IOException {
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        Path normalizedPath = pathToFile.normalize();
        Blob blob = new Blob(normalizedPath, repository);
        String hash = blob.getHash();
        addFileToIndex(normalizedPath, hash);

        return hash;
    }

    /**
     * Создание дерева структуры файлов и папок репозитория.
     * @return дерево с структурой папок.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws NotRegularFileException исключение, если вдруг объект Blob создается не от файла.
     */
    public @NotNull Tree createTreeFromIndex() throws IOException, NotRegularFileException {
        HashMap<Path, Tree> trees = new HashMap<>();
        Path rootPath = Paths.get(".").normalize();
        trees.put(rootPath, new Tree(rootPath));
        for (Path pathToFile : index.keySet()) {
            Path absolutePrefix = rootPath;
            for (Path prefix : pathToFile) {
                absolutePrefix = absolutePrefix.resolve(prefix);

                Tree selectedTree;
                if (absolutePrefix.equals(pathToFile)) {
                    Blob blob = new Blob(pathToFile, repository);

                    if (pathToFile.getParent() == null) {
                        selectedTree = trees.get(rootPath);
                    } else {
                        selectedTree = trees.get(pathToFile.getParent());
                    }

                    selectedTree.addBlob(blob, pathToFile.toString());
                } else {
                    if (!trees.containsKey(absolutePrefix)) {
                        Tree prefixTree = new Tree(prefix);
                        trees.put(absolutePrefix, prefixTree);
                        if (absolutePrefix.getParent() != null) {
                            selectedTree = trees.get(absolutePrefix.getParent());
                        } else {
                            selectedTree = trees.get(rootPath);
                        }

                        selectedTree.addChild(prefixTree);
                    }
                }
            }
        }

        Tree resTree = trees.get(rootPath);
        resTree.computeHash(repository);
        return resTree;
    }

    /**
     * Реализация команды commit.
     * @param author автор коммита
     * @param message сообщение при коммите
     * @throws NotRegularFileException аналогично исключению из createTreeFromIndex()
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void commitChanges(@NotNull String author, @NotNull String message)
                              throws NotRegularFileException, IOException {
        Tree tree = createTreeFromIndex();
        Reference ref = new Reference(repository);
        ArrayList<String> parentCommits;
        if (ref.getCommitHash().equals("")) {
            parentCommits = new ArrayList<>();
        } else {
            parentCommits = new ArrayList<>(Collections.singletonList(ref.getCommitHash()));
        }
        Commit commit = new Commit(author, message, tree.getHash(), parentCommits, repository);
        ref.refreshCommitHash(commit.getHash(), repository);
    }

    /**
     * Реалиация команды checkout, когда нужно создать новую ветку.
     * @param newBranchName имя новой ветки
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws BranchAlreadyExistsException если ветка уже существует.
     */
    public void checkoutToNewBranch(@NotNull String newBranchName) throws IOException, BranchAlreadyExistsException {
        String currentHead = repository.getCurrentHead();
        if (currentHead.startsWith(REFERENCE_PREFIX)) {
            Reference currentReference = new Reference(repository);
            String commitHash = currentReference.getCommitHash();
            repository.createNewBranch(newBranchName, commitHash);
        } else {
            repository.createNewBranch(newBranchName, currentHead);
        }
    }

    /**
     * Реализация команды checkout системы контроля версий.
     * @param revision название ветки или хеш коммита
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws NoRevisionExistsException не существует ни ветки ни хеша коммита, на которые можно переключиться
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    public void checkout(@NotNull String revision) throws IOException, ClassNotFoundException, NoRevisionExistsException {
        if (!repository.isCommitExists(revision) && !repository.isBranchExists(revision)) {
            throw new NoRevisionExistsException();
        }

        Reference currentReference = new Reference(repository);
        String commitHash = currentReference.getCommitHash();
        Commit commit = new Commit(commitHash, repository);
        Tree tree = new Tree(commit.getTreeHash(), repository);
        cleanCurrentCommit(tree);

        if (repository.isBranchExists(revision)) {
            Reference newReference = new Reference(revision, repository);
            String commitHashToRestore = newReference.getCommitHash();
            restoreCommit(commitHashToRestore);
        } else {
            restoreCommit(revision);
        }

        repository.writeHead(revision);
    }

    /**
     * Удаление вообще состояния репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    private void cleanCurrentCommit(@NotNull Tree tree) throws IOException, ClassNotFoundException {
        for (ObjectWithName<Blob> file : tree.getBlobFiles()) {
            Path fileName = Paths.get(file.getName());
            fileName.toFile().delete();
            Path parentDirectory = fileName.getParent();
            if (parentDirectory == null) {
                parentDirectory = Paths.get(".");
            }

            if (parentDirectory.toFile().listFiles().length == 0) {
                parentDirectory.toFile().delete();
            }
        }

        for (Tree subTree : tree.getTreeFiles()) {
            cleanCurrentCommit(subTree);
        }
    }

    /**
     * Восстановление состояние репозитория по коммиту.
     * @param commitHash в хеше этого коммита находится восстанавливаемое состояние репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    private void restoreCommit(@NotNull String commitHash) throws IOException, ClassNotFoundException {
        Commit commit = new Commit(commitHash, repository);
        Tree tree = new Tree(commit.getTreeHash(), repository);
        index = new HashMap<>();
        addTree(tree, Paths.get("."));
        updateIndex();
    }

    /**
     * Восстановление состояния репозитория соотвествующее дереву.
     * @param tree в этом объекте хранится вся структура файлов и папок.ё
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void addTree(@NotNull Tree tree, @NotNull Path root) throws IOException {
        for (ObjectWithName<Blob> file : tree.getBlobFiles()) {
            Blob blob = file.getContent();
            Path fileName = Paths.get(file.getName());
            index.put(fileName, blob.getHash());
            blob.fillFileWithContent(fileName, repository); // перезапишет файл, даже если тот существует
        }

        for (Tree subTree : tree.getTreeFiles()) {
            Path treeDirectory = root.resolve(subTree.getPrefix());
            Files.createDirectory(treeDirectory);
            addTree(subTree, treeDirectory);
        }
    }

    /**
     * Удаление ветки в репозитории.
     * @param branchName имя ветки, которую нужно удалить.
     * @throws NoBranchExistsException исключение, если не существует удаляемой ветки.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws CannotDeleteCurrentBranchException исключение, если пользователь хочет удалить ветку, на которой
     * он находится в данный моментъ
     */
    public void deleteBranch(@NotNull String branchName)
                throws NoBranchExistsException, IOException, CannotDeleteCurrentBranchException {
        if (repository.isBranchExists(branchName)) {
            String head = repository.getCurrentHead();
            String currentBranch = head.substring(REFERENCE_PREFIX.length());
            if (currentBranch.equals(branchName)) {
                throw new CannotDeleteCurrentBranchException();
            }

            repository.deleteBranch(branchName);
        } else {
            throw new NoBranchExistsException();
        }
    }

    /**
     * Получение лога.
     * @return класс, знающий как выводить сообщения о коммитах.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    public @NotNull VcsLog getLog() throws IOException, ClassNotFoundException {
        Reference reference = new Reference(repository);
        String currentCommitHash = reference.getCommitHash();
        Commit currentCommit = new Commit(currentCommitHash, repository);

        HashSet<String> commitHashes = new HashSet<>();
        commitHashes.add(currentCommitHash);
        ArrayList<Commit> commits = new ArrayList<>();
        commits.add(currentCommit);

        dfs(commits, commitHashes, currentCommit);
        return new VcsLog(commits);
    }

    /**
     * Обход дерева коммитов, породивших конкретный.
     * @param commits общий список, куда складываются неповторяющиеся коммиты.
     * @param commitHashes список хешей добавленных коммитов.
     * @param currentCommit предков этого коммита обходит dfs.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    private void dfs(@NotNull ArrayList<Commit> commits, @NotNull HashSet<String> commitHashes,
                     @NotNull Commit currentCommit) throws IOException, ClassNotFoundException {
        for (String commitHash : currentCommit.getParentCommits()) {
            if (!commitHashes.contains(commitHash)) {
                Commit parentCommit = new Commit(commitHash, repository);
                commits.add(parentCommit);
                commitHashes.add(commitHash);
                dfs(commits, commitHashes, parentCommit);
            }
        }
    }

    /**
     * Реализация команды merge.
     * @param branchName имя ветку, которую нужно влить в текующую.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    public void merge(@NotNull String branchName) throws IOException, ClassNotFoundException, NotRegularFileException {
        Reference newReference = new Reference(branchName, repository);
        String commitHash = newReference.getCommitHash();
        Commit commit = new Commit(commitHash, repository);
        Tree branchTree = new Tree(commit.getTreeHash(), repository);
        Tree currentTree = createTreeFromIndex();
        currentTree.mergeWith(branchTree);
        createIndexFromTree(currentTree);
        restoreCommit(commitHash);
        commitChanges(USERNAME, MERGE_MESSAGE + commitHash);
    }

    private void createIndexFromTree(Tree tree) {
        index.clear();
        HashSet<ObjectWithName<Blob>> files = tree.getAllFiles();
        for (ObjectWithName<Blob> file : files) {
            Blob blob = file.getContent();
            String filePath = file.getName();
            String fileHash = blob.getHash();
            index.put(Paths.get(filePath), fileHash);
        }
    }
}