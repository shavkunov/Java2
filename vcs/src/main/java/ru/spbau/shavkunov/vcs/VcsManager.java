package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.exceptions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static ru.spbau.shavkunov.vcs.Constants.*;

/**
 * Класс отвечающий за логику взаимодействия между пользователем и репозиторием.
 */
public class VcsManager {
    /**
     * Логгер этого класса.
     */
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(VcsManager.class);

    /**
     * Репозитоий, который упрявляется менеджером.
     */
    private @NotNull Repository repository;

    /**
     * Представление файла индекса -- множество путей файлов и их хешей.
     */
    private @NotNull Map<Path, String> index;

    /**
     * Добавленные файлы.
     */
    private @Nullable ArrayList<String> stagedFiles;

    /**
     * Удаленные файлы.
     */
    private @Nullable ArrayList<String> deletedFiles;

    /**
     * Измененные файлы.
     */
    private @Nullable ArrayList<String> modifiedFiles;

    /**
     * Чтение файла index, отвечающий за состояние репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void readIndex() throws IOException {
        logger.debug("Reading index file");

        index = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(repository.getIndexPath().toFile()));
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
    }

    /**
     * Сохранение изменений в файл index.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void updateIndex() throws IOException {
        logger.debug("Updating index file");

        FileWriter fileWriter = new FileWriter(repository.getIndexPath().toFile());
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

    public VcsManager(@NotNull Repository repository) throws IOException {
        logger.debug("---------------------------Manager was created---------------------------");
        this.repository = repository;
        readIndex();
    }

    public VcsManager(@NotNull Path pathToRepo) throws IOException {
        this.repository = new Repository(pathToRepo);
        readIndex();
    }

    /**
     * Добавить файл в index.
     * @param pathToFile путь к файлу.
     * @param hash хеш добавляемого файла.
     */
    private void addFileToIndex(@NotNull Path pathToFile, @NotNull String hash) throws IOException {
        logger.debug("Adding file " + pathToFile + " to index");
        index.put(pathToFile, hash);
        updateIndex();
    }

    /**
     * Удалить файл из index.
     * @param pathToFile путь к файлу.
     */
    private void removeFileFromIndex(@NotNull Path pathToFile) throws IOException {
        logger.debug("Adding file " + pathToFile + " from index");
        index.remove(pathToFile);
        updateIndex();
    }

    /**
     * Реализация команды remove. Удаление файла из репозитория.
     * @param pathToFile путь к удаляемому файлу.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     */
    public void removeFile(@NotNull Path pathToFile) throws NotRegularFileException, IOException {
        logger.debug("Removing file " + pathToFile);
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        pathToFile.toFile().delete();
        removeFileFromIndex(pathToFile.normalize());
    }

    /**
     * Реализация команды add репозитория.
     * @param pathToFile путь к файлу, который нужно добавить.
     * @throws NotRegularFileException исключение, если путь оказался не к файлу.
     * @throws IOException исключение, если возникли проблемы с чтен ием файла.
     * @return хеш файла, добавленного в репозиторий.
     */
    public String addFile(@NotNull Path pathToFile) throws NotRegularFileException, IOException {
        logger.debug("Adding file " + pathToFile);
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
                    Blob blob = new Blob(pathToFile, repository);

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
            trees.get(path).computeHash(repository);
            logger.debug(trees.get(path).getHash());
        }
        VcsTree resVcsTree = trees.get(rootPath);

        logger.debug("Tree with hash " + resVcsTree.getHash() + " from index was created");
        return resVcsTree;
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
        logger.debug("Commiting...");
        VcsTree vcsTree = createTreeFromIndex();
        logger.debug("Created tree : " + vcsTree.getHash());
        Reference ref = new Reference(repository);
        ArrayList<String> parentCommits;
        if (ref.getCommitHash().equals("")) {
            parentCommits = new ArrayList<>();
        } else {
            parentCommits = new ArrayList<>(Collections.singletonList(ref.getCommitHash()));
        }
        Commit commit = new Commit(author, message, vcsTree.getHash(), parentCommits, repository);
        logger.debug("Commit : " + commit.getHash());
        ref.refreshCommitHash(commit.getHash(), repository);
        logger.debug("Committed file changes");
    }

    /**
     * Реалиация команды checkout, когда нужно создать новую ветку.
     * @param newBranchName имя новой ветки
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws BranchAlreadyExistsException если ветка уже существует.
     */
    public void checkoutToNewBranch(@NotNull String newBranchName) throws IOException, BranchAlreadyExistsException {
        logger.debug("Creating branch " + newBranchName + "...");
        String currentHead = repository.getCurrentHead();
        if (currentHead.startsWith(REFERENCE_PREFIX)) {
            Reference currentReference = new Reference(repository);
            String commitHash = currentReference.getCommitHash();
            repository.createNewBranch(newBranchName, commitHash);
        } else {
            repository.createNewBranch(newBranchName, currentHead);
        }

        logger.debug("new branch was created : " + newBranchName);
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
            logger.error("Error : no correct commit hash revision or correct branch name provided");
            throw new NoRevisionExistsException();
        }

        logger.debug("Checkout to " + revision);
        Reference currentReference = new Reference(repository);
        String commitHash = currentReference.getCommitHash();
        Commit commit = new Commit(commitHash, repository);
        VcsTree vcsTree = new VcsTree(commit.getTreeHash(), repository);
        logger.debug("Clearing current commit...");
        cleanCurrentCommit(vcsTree);

        if (repository.isBranchExists(revision)) {
            Reference newReference = new Reference(revision, repository);
            String commitHashToRestore = newReference.getCommitHash();
            restoreCommit(commitHashToRestore);
            logger.debug("Checkouted to branch " + revision);
        } else {
            restoreCommit(revision);
            logger.debug("Checkouted to revision " + revision);
        }

        repository.writeHead(revision);
    }

    /**
     * Удаление вообще состояния репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    private void cleanCurrentCommit(@NotNull VcsTree vcsTree) throws IOException, ClassNotFoundException {
        for (ObjectWithName<Blob> file : vcsTree.getBlobFiles()) {
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

        for (VcsTree subVcsTree : vcsTree.getVcsTreeFiles()) {
            cleanCurrentCommit(subVcsTree);
        }
    }

    /**
     * Восстановление состояние репозитория по коммиту.
     * @param commitHash в хеше этого коммита находится восстанавливаемое состояние репозитория.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws ClassNotFoundException если возникли проблемы с десериализацией.
     */
    private void restoreCommit(@NotNull String commitHash) throws IOException, ClassNotFoundException {
        logger.debug("Restoring commit " + commitHash);
        Commit commit = new Commit(commitHash, repository);
        VcsTree vcsTree = new VcsTree(commit.getTreeHash(), repository);
        index = new HashMap<>();
        addTree(vcsTree, Paths.get("."));
        updateIndex();
    }

    /**
     * Добавление в корневую папку дополнительную структуру файлов.
     * @param vcsTree в этом объекте хранится вся структура файлов и папок.
     * @param root корневой путь, куда нужно добавить дерево
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    private void addTree(@NotNull VcsTree vcsTree, @NotNull Path root) throws IOException {
        logger.debug("Adding tree with hash " + vcsTree.getHash() + " to " + root);
        for (ObjectWithName<Blob> file : vcsTree.getBlobFiles()) {
            Blob blob = file.getContent();
            Path fileName = Paths.get(file.getName());
            index.put(fileName, blob.getHash());
            blob.fillFileWithContent(fileName, repository); // перезапишет файл, даже если тот существует
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
     * Удаление ветки в репозитории.
     * @param branchName имя ветки, которую нужно удалить.
     * @throws NoBranchExistsException исключение, если не существует удаляемой ветки.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     * @throws CannotDeleteCurrentBranchException исключение, если пользователь хочет удалить ветку, на которой
     * он находится в данный моментъ
     */
    public void deleteBranch(@NotNull String branchName)
                throws NoBranchExistsException, IOException, CannotDeleteCurrentBranchException {
        logger.debug("Trying to delete branch " + branchName);
        if (repository.isBranchExists(branchName)) {
            String head = repository.getCurrentHead();
            String currentBranch = head.substring(REFERENCE_PREFIX.length());
            if (currentBranch.equals(branchName)) {
                logger.error("Tried to delete current branch");
                throw new CannotDeleteCurrentBranchException();
            }

            repository.deleteBranch(branchName);
            logger.debug("Deleted branch " + branchName);
        } else {
            logger.error("Branch with name " + branchName + " doesn't exist");
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
        logger.debug("Creating vcs log");
        Reference reference = new Reference(repository);
        String currentCommitHash = reference.getCommitHash();
        Commit currentCommit = new Commit(currentCommitHash, repository);

        HashSet<String> commitHashes = new HashSet<>();
        commitHashes.add(currentCommitHash);
        ArrayList<Commit> commits = new ArrayList<>();
        commits.add(currentCommit);

        logger.debug("Started dfs walk through graph of commits");
        dfs(commits, commitHashes, currentCommit);

        logger.debug("Log was created");
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
        logger.debug("Merge " + branchName + " into " + repository.getCurrentHead());
        Reference newReference = new Reference(branchName, repository);
        String commitHash = newReference.getCommitHash();
        Commit commit = new Commit(commitHash, repository);
        VcsTree branchVcsTree = new VcsTree(commit.getTreeHash(), repository);

        Reference reference = new Reference(repository);
        String currentCommitHash = reference.getCommitHash();
        Commit currentCommit = new Commit(currentCommitHash, repository);
        VcsTree currentVcsTree = new VcsTree(currentCommit.getTreeHash(), repository);

        currentVcsTree.mergeWith(branchVcsTree);
        createIndexFromTree(currentVcsTree);
        restoreCommit(commitHash);
        commitChanges(USERNAME, MERGE_MESSAGE + commitHash);
        logger.debug("Created merge commit");
    }

    /**
     * Создание файла index из дерева.
     * @param vcsTree дерево файлов.
     */
    private void createIndexFromTree(@NotNull VcsTree vcsTree) {
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
     * Получение имен файлов из дерева.
     * @param tree дерево файлов.
     * @return множество путей к файлам.
     */
    private Set<String> getFilesNames(@NotNull VcsTree tree) {
        return tree.getAllFiles()
                   .stream()
                   .map(blobObjectWithName -> blobObjectWithName.getName())
                   .collect(Collectors.toSet());
    }

    /**
     * Получение имен файлов с их хешами.
     * @param tree дерево файлов.
     * @return отображение из пути к файла к его хешу.
     */
    private Map<String, String> getPathWithHashes(@NotNull VcsTree tree) {
        return tree.getAllFiles()
                   .stream()
                   .collect(Collectors.toMap(ObjectWithName::getName,
                                             objectWithName -> objectWithName.getContent().getHash()));
    }

    /**
     * Реализация команды status. Вывод всех измененных/удаленных/недобавленных/добавленных файлов.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     */
    public void status() throws IOException, NotRegularFileException,
                                ClassNotFoundException, NoRootDirectoryExistsException {
        printUntrackedFiles();
        getStatusFiles();

        printList(MODIFIED_MESSAGE, modifiedFiles);
        printList(STAGED_MESSAGE, stagedFiles);
        printList(DELETED_MESSAGE, deletedFiles);
    }

    /**
     * Вывод на экран списка с сообщением.
     * @param message сообщение перед выводом списка.
     * @param list список, который нужно вывести(каждый элемент на новой строке)
     */
    private void printList(@NotNull String message, @NotNull ArrayList<String> list) {
        if (list.size() > 0) {
            System.out.println(message);
            for (String item : list) {
                System.out.println(item);
            }

            System.out.println();
        }
    }

    /**
     * Получение файлов, который находятся под контролем системы версий.
     * @return множество путей к файлам.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    private @NotNull HashSet<String> getTrackedFiles() throws IOException, ClassNotFoundException,
                                                              NotRegularFileException {
        VcsTree currentVcsTree = createTreeFromIndex();
        VcsTree commitVcsTree = new VcsTree(new Commit(new Reference(repository).getCommitHash(),
                repository).getTreeHash(), repository);

        Set<String> currentFileNames = getFilesNames(currentVcsTree);
        Set<String> commitVcsTreeFiles = getFilesNames(commitVcsTree);

        HashSet<String> union = new HashSet<>();
        union.addAll(currentFileNames);
        union.addAll(commitVcsTreeFiles);

        logger.debug("Tracked files : " + union);
        return union;
    }

    /**
     * Вывод файлов, не находящихся под контролем системы версий.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    private void printUntrackedFiles() throws NoRootDirectoryExistsException, NotRegularFileException,
                                              IOException, ClassNotFoundException {
        FilesTree filesTree = new FilesTree(Paths.get("."), getTrackedFiles());
        System.out.println(UNTRACKED_MESSAGE);
        filesTree.printTree(0);
        System.out.println();
    }

    /**
     * Получение файлов, не находящихся под контролем системы версий.
     * @return множество путей к файлам.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public @NotNull HashSet<String> getUntrackedFiles() throws NotRegularFileException, IOException,
                                                               ClassNotFoundException, NoRootDirectoryExistsException {
        FilesTree filesTree = new FilesTree(repository.getRootDirectory(), getTrackedFiles());
        logger.debug("Untracked files : " + filesTree.getAllFiles());
        return filesTree.getAllFiles();
    }

    /**
     * Получение файлов, по которым можно выдать результаты команды status.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    private void getStatusFiles() throws IOException, ClassNotFoundException, NotRegularFileException {
        logger.debug("Getting status information");
        VcsTree currentVcsTree = createTreeFromIndex();
        VcsTree commitVcsTree = new VcsTree(new Commit(new Reference(repository).getCommitHash(),
                repository).getTreeHash(), repository);

        Map<String, String> currentMap = getPathWithHashes(currentVcsTree);
        Map<String, String> commitMap = getPathWithHashes(commitVcsTree);
        modifiedFiles = new ArrayList<>();
        deletedFiles = new ArrayList<>();
        stagedFiles = new ArrayList<>();

        for (String path : currentMap.keySet()) {
            if (commitMap.containsKey(path)) {
                modifiedFiles.add(path);
            } else {
                stagedFiles.add(path);
            }
        }

        for (String path : commitMap.keySet()) {
            if (!currentMap.containsKey(path)) {
                deletedFiles.add(path);
            }
        }

        logger.debug("Modified files :" + modifiedFiles);
        logger.debug("Deleted files : " + deletedFiles);
        logger.debug("Staged files : " + stagedFiles);
    }

    /**
     * Получение добавленных файлов.
     * @return множество путей к файлам.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public @NotNull ArrayList<String> getStagedFiles() throws NotRegularFileException, IOException,
                                                              ClassNotFoundException {
        if (stagedFiles == null) {
            getStatusFiles();
        }

        return stagedFiles;
    }

    /**
     * Получение удаленных файлов.
     * @return множество путей к файлам.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public @NotNull ArrayList<String> getDeletedFiles() throws NotRegularFileException, IOException,
                                                               ClassNotFoundException {
        if (deletedFiles == null) {
            getStatusFiles();
        }

        return deletedFiles;
    }

    /**
     * Получение изменных файлов.
     * @return множество путей к файлам.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public @NotNull ArrayList<String> getModifiedFiles() throws NotRegularFileException, IOException,
                                                                ClassNotFoundException {
        if (modifiedFiles == null) {
            getStatusFiles();
        }

        return modifiedFiles;
    }

    /**
     * Реализация команды reset. Восстанавливает файл до состояния коммита.
     * @param pathToFile путь к файлу.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public void reset(@NotNull Path pathToFile) throws IOException, ClassNotFoundException {
        logger.debug("Reseting file " + pathToFile);
        VcsTree commitVcsTree = new VcsTree(new Commit(new Reference(repository).getCommitHash(),
                repository).getTreeHash(), repository);

        String fileHash = commitVcsTree.getFileHash(pathToFile);
        if (fileHash != null) {
            repository.restoreFile(pathToFile, fileHash);
            logger.debug("File was restored");
        } else {
            logger.error("No file with name " + pathToFile + " in repository");
            throw new NoSuchFileException(pathToFile.toString());
        }
    }

    /**
     * Реализация команды clean. Удаление всех файлов, не находящихся под контролем системы версий.
     * @throws NoRootDirectoryExistsException исключение, если не была найдена корневая директория репозитория.
     * @throws IOException исключение, если возникли проблемы с файлом.
     * @throws NotRegularFileException исключение, если ожидали файл, а получили директорию.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public void clean() throws ClassNotFoundException, NotRegularFileException,
                               NoRootDirectoryExistsException, IOException {
        logger.debug("Cleaning repository");
        HashSet<String> untrackedFiles = getUntrackedFiles();
        for (String path : untrackedFiles) {
            File file = repository.getRootDirectory().resolve(path).toFile();
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }
}