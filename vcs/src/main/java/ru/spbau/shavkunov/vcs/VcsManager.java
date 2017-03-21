package ru.spbau.shavkunov.vcs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VcsManager {
    private Repository repository;

    public VcsManager(Repository repository) {
        this.repository = repository;
    }

    public VcsManager(Path pathToRepo) throws IOException {
        this.repository = new Repository(pathToRepo);
    }

    private void addFileToIndex(Path pathToFile, String hash) throws IOException {
        Path index = repository.getIndexPath();
        BufferedWriter out = new BufferedWriter(new FileWriter(index.toFile()));
        out.write(pathToFile.toString() + " " + hash);
        out.close();
    }

    public void addFile(Path pathToFile) throws NotRegularFileException, IOException {
        if (Files.isDirectory(pathToFile)) {
            throw new NotRegularFileException();
        }

        Blob blob = new Blob(pathToFile);
        String hash = blob.getHash();
        addFileToIndex(pathToFile, hash);
    }

    private Tree createTreeFromIndex() {
        return null;
    }

    public void commit(String author, String message) {
        Tree tree = createTreeFromIndex();
        String treeHash = tree.getHash();

    }
}