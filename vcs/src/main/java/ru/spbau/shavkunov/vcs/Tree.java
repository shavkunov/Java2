package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

/**
 * Класс, отвечающий за представление структуры папок и файлов в репозитории.
 */
public class Tree extends VcsObjectWithHash implements Serializable {
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

    /**
     * Отступ при печати дерева.
     */
    private static int DEFAULT_INDENT = 2;

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

        hash = treeHash;
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

    public void computeHash(Repository repository) throws IOException {
        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    private boolean isFileExists(Path rootPath, Path pathToFile) {
        for (ObjectWithName<Blob> blob : blobFiles) {
            if (blob.getName().equals(pathToFile.toString())) {
                return true;
            }
        }

        for (Tree subTree : treeFiles) {
            Path subDirectory = rootPath.resolve(subTree.getPrefix());
            if (pathToFile.toString().startsWith(subDirectory.toString())) {
                return subTree.isFileExists(subDirectory, pathToFile);
            }
        }

        return false;
    }

    public void printTree(int spaces) {
        String indent = multiply("-", spaces);
        for (ObjectWithName<Blob> blob : blobFiles) {
            System.out.println(indent + blob.getName());
        }

        for (Tree subTree : treeFiles) {
            System.out.println(indent + subTree.getPrefix());
            subTree.printTree(spaces + DEFAULT_INDENT);
        }
    }

    /**
     * Поиск файла в дереве
     * @param pathToFile путь искомого файла
     * @return true, если файл присутствует, иначе false
     */
    public boolean isFileExists(Path pathToFile) {
        return isFileExists(Paths.get(""), pathToFile);
    }

    private static @NotNull String multiply(@NotNull String sample, int amount) {
        String copy = "";

        for (int i = 0; i < amount; i++) {
            copy += sample;
        }

        return copy;
    }

    public @NotNull HashSet<ObjectWithName<Blob>> getBlobFiles() {
        return blobFiles;
    }

    public @NotNull HashSet<Tree> getTreeFiles() {
        return treeFiles;
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

    public @NotNull HashSet<ObjectWithName<Blob>> getAllFiles() {
        HashSet<ObjectWithName<Blob>> result = new HashSet<>();
        result.addAll(blobFiles);

        for (Tree subTree : treeFiles) {
            result.addAll(subTree.getAllFiles());
        }

        return result;
    }

    public void mergeWith(Tree tree) {
        for (ObjectWithName<Blob> blob : tree.getBlobFiles()) {
            blobFiles.add(blob);
        }

        HashMap<String, Tree> prefixWithTree = new HashMap<>();
        for (Tree subTree : treeFiles) {
            prefixWithTree.put(subTree.getPrefix(), subTree);
        }

        for (Tree subTree : tree.getTreeFiles()) {
            if (prefixWithTree.containsKey(subTree.getPrefix())) {
                Tree selectedTree = prefixWithTree.get(subTree.getPrefix());
                selectedTree.mergeWith(subTree);
            } else {
                this.addChild(subTree);
            }
        }
    }
}