package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

/**
 * Класс, отвечающий за представление структуры папок и файлов в репозитории.
 */
public class Tree extends VcsObjectWithHash {
    /**
     * Список файлов с их именами(т.е. с путями к этим файлам) на текущем уровне.
     */
    private @NotNull HashSet<ObjectWithName<Blob>> blobFiles;

    /**
     * Список деревьев, располженных уровнями ниже.
     */
    private @NotNull HashSet<Tree> treeFiles;

    /**
     * Название папки, в которой находится текущее дерево.
     */
    private @NotNull String prefix;

    public @NotNull String getPrefix() {
        return prefix;
    }

    public Tree(@NotNull HashSet<ObjectWithName<Blob>> blobFiles, @NotNull HashSet<Tree> treeFiles,
                @NotNull Repository repository, @NotNull Path prefix) throws IOException {
        this.blobFiles = blobFiles;
        this.treeFiles = treeFiles;
        this.prefix = prefix.toString();

        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    public Tree(@NotNull Path prefix) {
        blobFiles = new HashSet<>();
        treeFiles = new HashSet<>();
        this.prefix = prefix.toString();
    }

    public Tree(@NotNull String treeHash, @NotNull Repository repository) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(repository.getObjectsPath().resolve(treeHash));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            blobFiles = (HashSet<ObjectWithName<Blob>>) input.readObject();
            treeFiles = (HashSet<Tree>) input.readObject();
            prefix = (String) input.readObject();
        }
    }

    public @NotNull HashSet<ObjectWithName<Blob>> getBlobFiles() {
        return blobFiles;
    }

    public @NotNull HashSet<Tree> getTreeFiles() {
        return treeFiles;
    }

    private @NotNull byte[] getContent() throws IOException {
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

    public void addBlob(@NotNull Blob blob, @NotNull String name) {
        blobFiles.add(new ObjectWithName<>(blob, name));
    }

    public void addChild(@NotNull Tree tree) {
        treeFiles.add(tree);
    }


    @Override
    public @NotNull Path getPathToObject(@NotNull Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}