package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Commit extends VcsObjectWithHash {
    private Date date;
    private String author;
    private String message;
    private String treeHash;
    private List<String> parentCommits;

    public Commit(String author, String message, String treeHash, List<String> parentCommits, Repository repository)
                 throws IOException {
        this.date = new Date();
        this.author = author;
        this.message = message;
        this.treeHash = treeHash;
        this.parentCommits = parentCommits;

        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    public String getTreeHash() {
        return treeHash;
    }

    public Commit(String commitHash, Repository repository) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(repository.getObjectsPath().resolve(commitHash));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            date = (Date) input.readObject();
            author = (String) input.readObject();
            message = (String) input.readObject();
            treeHash = (String) input.readObject();
            parentCommits = (List<String>) input.readObject();
        }
    }

    private byte[] getContent() throws IOException {
        byte[] res;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeObject(date);
            output.writeObject(author);
            output.writeObject(message);
            output.writeObject(treeHash);
            output.writeObject(parentCommits);

            output.flush();
            res = byteArrayOutputStream.toByteArray();
        }

        return res;
    }

    public List<String> getParentCommits() {
        return parentCommits;
    }

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}