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
public class VcsTree extends VcsObjectWithHash implements Tree, Serializable {
    /**
     * Список файлов с их именами(т.е. с путями к этим файлам) на текущем уровне.
     */
    private @NotNull HashSet<ObjectWithName<Blob>> blobFiles;

    /**
     * Список деревьев, располженных уровнями ниже.
     */
    private @NotNull HashSet<VcsTree> vcsTreeFiles;

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

    public VcsTree(@NotNull Path prefix) {
        blobFiles = new HashSet<>();
        vcsTreeFiles = new HashSet<>();

        if (prefix.toString().equals("")) {
            this.prefix = ".";
        } else {
            this.prefix = prefix.toString();
        }
    }

    public VcsTree(@NotNull String treeHash, @NotNull Repository repository) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(repository.getObjectsPath().resolve(treeHash));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            blobFiles = (HashSet<ObjectWithName<Blob>>) input.readObject();
            vcsTreeFiles = (HashSet<VcsTree>) input.readObject();
            prefix = (String) input.readObject();
        }

        hash = treeHash;
    }

    private @NotNull byte[] getContent() throws IOException {
        byte[] content;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeObject(blobFiles);
            output.writeObject(vcsTreeFiles);
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

    private String getFileHash(Path rootPath, Path pathToFile) {
        for (ObjectWithName<Blob> blob : blobFiles) {
            if (blob.getName().equals(pathToFile.toString())) {
                return blob.getContent().getHash();
            }
        }

        for (VcsTree subVcsTree : vcsTreeFiles) {
            Path subDirectory = rootPath.resolve(subVcsTree.getPrefix());
            if (pathToFile.toString().startsWith(subDirectory.toString())) {
                return subVcsTree.getFileHash(subDirectory, pathToFile);
            }
        }

        return null;
    }

    public String getFileHash(Path pathToFile) {
        return getFileHash(Paths.get("."), pathToFile);
    }

    @Override
    public void printTree(int spaces) {
        String indent = Tree.multiply("-", spaces + 1);
        String directoryIndent = Tree.multiply("-", spaces);
        System.out.println(directoryIndent + prefix);

        for (ObjectWithName<Blob> blob : blobFiles) {
            System.out.println(indent + blob.getName());
        }

        for (VcsTree subVcsTree : vcsTreeFiles) {
            System.out.println(indent + subVcsTree.getPrefix());
            subVcsTree.printTree(spaces + DEFAULT_INDENT);
        }
    }

    /**
     * Поиск файла в дереве
     * @param pathToFile путь искомого файла
     * @return true, если файл присутствует, иначе false
     */
    @Override
    public boolean isFileExists(Path pathToFile) {
        return getFileHash(pathToFile) != null;
    }

    public @NotNull HashSet<ObjectWithName<Blob>> getBlobFiles() {
        return blobFiles;
    }

    public @NotNull HashSet<VcsTree> getVcsTreeFiles() {
        return vcsTreeFiles;
    }

    public void addBlob(@NotNull Blob blob, @NotNull String name) {
        blobFiles.add(new ObjectWithName<>(blob, name));
    }

    public void addChild(@NotNull VcsTree vcsTree) {
        vcsTreeFiles.add(vcsTree);
    }


    @Override
    public @NotNull Path getPathToObject(@NotNull Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }

    public @NotNull HashSet<ObjectWithName<Blob>> getAllFiles() {
        HashSet<ObjectWithName<Blob>> result = new HashSet<>();
        result.addAll(blobFiles);

        for (VcsTree subVcsTree : vcsTreeFiles) {
            result.addAll(subVcsTree.getAllFiles());
        }

        return result;
    }

    public void mergeWith(VcsTree vcsTree) {
        for (ObjectWithName<Blob> blob : vcsTree.getBlobFiles()) {
            blobFiles.add(blob);
        }

        HashMap<String, VcsTree> prefixWithTree = new HashMap<>();
        for (VcsTree subVcsTree : vcsTreeFiles) {
            prefixWithTree.put(subVcsTree.getPrefix(), subVcsTree);
        }

        for (VcsTree subVcsTree : vcsTree.getVcsTreeFiles()) {
            if (prefixWithTree.containsKey(subVcsTree.getPrefix())) {
                VcsTree selectedVcsTree = prefixWithTree.get(subVcsTree.getPrefix());
                selectedVcsTree.mergeWith(subVcsTree);
            } else {
                this.addChild(subVcsTree);
            }
        }
    }
}