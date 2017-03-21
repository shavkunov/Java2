package ru.spbau.shavkunov.vcs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        trees.put(rootPath, new Tree());
        for (Path pathToFile : index.keySet()) {
            Path absolutePrefix = rootPath;
            for (Path prefix : pathToFile) {
                absolutePrefix = absolutePrefix.resolve(prefix);
                if (!trees.containsKey(absolutePrefix)) {
                    Tree prefixTree = new Tree();
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

    public void commitChanges(String author, String message) throws IOException, NotRegularFileException {
        Tree tree = createTreeFromIndex();
        Reference ref = new Reference(repository);
        ArrayList<String> parentCommits = new ArrayList<>(Collections.singletonList(ref.getCommitHash()));
        Commit commit = new Commit(author, message, tree.getHash(), parentCommits, repository);
        ref.refreshCommitHash(commit.getHash(), repository);
    }
}