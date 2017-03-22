package ru.spbau.shavkunov.vcs;

import ru.spbau.shavkunov.vcs.exceptions.CannotDeleteCurrentBranchException;
import ru.spbau.shavkunov.vcs.exceptions.NoBranchExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.spbau.shavkunov.vcs.Constants.REFERENCE_PREFIX;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;

// TODO : normalize paths
public class VcsManager {
    private Repository repository;
    private Map<Path, String> index;

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

    public VcsManager(Repository repository) throws IOException {
        this.repository = repository;
        readIndex();
    }

    public VcsManager(Path pathToRepo) throws IOException {
        this.repository = new Repository(pathToRepo);
        readIndex();
    }

    private void addFileToIndex(Path pathToFile, String hash) {
        index.put(pathToFile, hash);
    }

    private void removeFileFromIndex(Path pathToFile) {
        index.remove(pathToFile);
    }

    public void removeFile(Path pathToFile) throws NotRegularFileException {
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        removeFileFromIndex(pathToFile);
    }

    public void addFile(Path pathToFile) throws NotRegularFileException, IOException {
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        Blob blob = new Blob(pathToFile, repository);
        String hash = blob.getHash();
        addFileToIndex(pathToFile, hash);
    }

    private Tree createTreeFromIndex() throws IOException, NotRegularFileException {
        HashMap<Path, Tree> trees = new HashMap<>();
        Path rootPath = Paths.get("");
        trees.put(rootPath, new Tree(rootPath));
        for (Path pathToFile : index.keySet()) {
            Path absolutePrefix = rootPath;
            for (Path prefix : pathToFile) {
                absolutePrefix = absolutePrefix.resolve(prefix);
                if (!trees.containsKey(absolutePrefix)) {
                    Tree prefixTree = new Tree(prefix);
                    trees.put(absolutePrefix, prefixTree);
                    if (absolutePrefix.getParent() != null) {
                        trees.get(absolutePrefix.getParent()).addChild(prefixTree);
                    }
                }

                if (absolutePrefix.equals(pathToFile)) {
                    Blob blob = new Blob(pathToFile, repository);
                    trees.get(pathToFile).addBlob(blob, pathToFile.toString());
                }
            }
        }

        return trees.get(rootPath);
    }

    /**
     * Реализация команды commit.
     * @param author автор коммита
     * @param message сообщение при коммите
     * @throws Exception
     * @throws NotRegularFileException
     */
    public void commitChanges(String author, String message) throws Exception, NotRegularFileException {
        Tree tree = createTreeFromIndex();
        Reference ref = new Reference(repository);
        ArrayList<String> parentCommits = new ArrayList<>(Collections.singletonList(ref.getCommitHash()));
        Commit commit = new Commit(author, message, tree.getHash(), parentCommits, repository);
        ref.refreshCommitHash(commit.getHash(), repository);
    }

    /**
     * Реалиация команды checkout, когда нужно создать новую ветку.
     * @param newBranchName имя новой ветки
     * @throws Exception что-то пошло не так
     */
    public void checkoutToNewBranch(String newBranchName) throws Exception {
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
     * @throws Exception что-то пошло не так TODO
     * @throws NoBranchExistsException не существует ветки, на которую нужно переключиться
     */
    public void checkout(String revision) throws Exception, NoBranchExistsException {
        if (revision.startsWith(REFERENCE_PREFIX)) {
            String branchName = revision.substring(REFERENCE_PREFIX.length());
            if (repository.isBranchExists(branchName)) {
                Reference newReference = new Reference(branchName, repository);
                String commitHash = newReference.getCommitHash();
                cleanAll();
                restoreCommit(commitHash);
            } else {
                throw new NoBranchExistsException();
            }
        } else {
            cleanAll();
            restoreCommit(revision);
        }

        repository.writeHead(revision);
    }

    /**
     * Удаление вообще состояния репозитория.
     */
    private void cleanAll() {
        Path root = Paths.get("");
        for (File file : root.toFile().listFiles()) {
            if (!file.getName().equals(VCS_FOLDER)) {
                file.delete(); // папки удаляются рекурсивно?
            }
        }
    }

    /**
     * Восстановление состояние репозитория по коммиту.
     * @param commitHash в хеше этого коммита находится восстанавливаемое состояние репозитория.
     */
    private void restoreCommit(String commitHash) throws IOException, ClassNotFoundException {
        Commit commit = new Commit(commitHash, repository);
        Tree tree = new Tree(commit.getTreeHash(), repository);
        restoreTree(tree, Paths.get(""));
    }

    /**
     * Восстановление состояния репозитория соотвествующее дереву.
     * @param tree в этом объекте хранится вся структура файлов и папок.
     */
    private void restoreTree(Tree tree, Path root) throws IOException {
        for (ObjectWithName<Blob> file : tree.getBlobFiles()) {
            Blob blob = file.getContent();
            Path fileName = Paths.get(file.getName());
            blob.fillFileWithContent(fileName, repository);
        }

        for (Tree subTree : tree.getTreeFiles()) {
            Path treeDirectory = root.resolve(subTree.getPrefix());
            Files.createDirectory(treeDirectory);
            restoreTree(subTree, treeDirectory);
        }
    }

    public void deleteBranch(String branchName) throws NoBranchExistsException, IOException, CannotDeleteCurrentBranchException {
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
}