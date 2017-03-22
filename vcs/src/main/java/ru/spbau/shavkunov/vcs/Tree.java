package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Tree extends VcsObjectWithHash {
    private HashSet<ObjectWithName<Blob>> blobFiles;
    private HashSet<Tree> treeFiles;

    public String getPrefix() {
        return prefix;
    }

    private String prefix;

    public Tree(HashSet<ObjectWithName<Blob>> blobFiles, HashSet<Tree> treeFiles, Repository repository, Path prefix)
                throws IOException {
        this.blobFiles = blobFiles;
        this.treeFiles = treeFiles;
        this.prefix = prefix.toString();

        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    public Tree(Path prefix) {
        blobFiles = new HashSet<>();
        treeFiles = new HashSet<>();
        this.prefix = prefix.toString();
    }

    public Tree(String treeHash, Repository repository) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(repository.getObjectsPath().resolve(treeHash));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            blobFiles = (HashSet<ObjectWithName<Blob>>) input.readObject();
            treeFiles = (HashSet<Tree>) input.readObject();
            prefix = (String) input.readObject();
        }
    }

    public HashSet<ObjectWithName<Blob>> getBlobFiles() {
        return blobFiles;
    }

    public HashSet<Tree> getTreeFiles() {
        return treeFiles;
    }

    private byte[] getContent() throws IOException {
        byte[] content;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeObject(blobFiles);
            output.writeObject(treeFiles);
            output.writeObject(prefix);
            output.flush();
            content = byteArrayOutputStream.toByteArray();
        }

        return content;
    }

    public void addBlob(Blob blob, String name) {
        blobFiles.add(new ObjectWithName<>(blob, name));
    }

    public void addChild(Tree tree) {
        treeFiles.add(tree);
    }


    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}