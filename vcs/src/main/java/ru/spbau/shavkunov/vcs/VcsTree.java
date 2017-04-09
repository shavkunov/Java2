package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Класс, отвечающий за представление структуры папок и файлов в репозитории.
 */
public class VcsTree extends VcsObjectWithHash implements Tree, Serializable, Comparable<VcsTree> {
    /**
     * Список файлов с их именами(т.е. с путями к этим файлам) на текущем уровне.
     */
    private @NotNull ArrayList<ObjectWithName<Blob>> blobFiles;

    /**
     * Список деревьев, располженных уровнями ниже.
     */
    private @NotNull ArrayList<VcsTree> vcsTreeFiles;

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
        blobFiles = new ArrayList<>();
        vcsTreeFiles = new ArrayList<>();

        if (prefix.toString().equals("")) {
            this.prefix = ".";
        } else {
            this.prefix = prefix.toString();
        }
    }

    /**
     * Получить дерево по его хешу.
     * @param treeHash хеш дерева.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws ClassNotFoundException исключение, если невозможно интерпретировать данные.
     */
    public VcsTree(@NotNull String treeHash, @NotNull byte[] content) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            blobFiles = (ArrayList<ObjectWithName<Blob>>) input.readObject();
            vcsTreeFiles = (ArrayList<VcsTree>) input.readObject();
            prefix = (String) input.readObject();
        }

        hash = treeHash;
    }

    @Override
    public @NotNull byte[] getContent() throws IOException {
        byte[] content;
        blobFiles.sort(ObjectWithName::compareTo);
        vcsTreeFiles.sort(VcsTree::compareTo);

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

    /**
     * Вычисление хеша дерева.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public void computeHash() throws IOException {
        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
    }

    /**
     * Получение хеша файла.
     * @param rootPath некоторый префикс файла.
     * @param pathToFile путь к файлу
     * @return хеш файла, если такой в дереве нашелся, иначе null.
     */
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

    /**
     * Получение хеша файла.
     * @param pathToFile путь к хешу.
     * @return null, если файла нет в дереве, иначе его хеш.
     */
    public String getFileHash(Path pathToFile) {
        return getFileHash(Paths.get("."), pathToFile);
    }

    // TODO : ссылка на интерфейс
    @Override
    public void printTree(int spaces) {
        String indent = Tree.multiply("-", spaces + 1);
        String directoryIndent = Tree.multiply("-", spaces);
        System.out.println(directoryIndent + prefix);

        for (ObjectWithName<Blob> blob : blobFiles) {
            System.out.println(indent + blob.getName());
        }

        for (VcsTree subVcsTree : vcsTreeFiles) {
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

    public @NotNull ArrayList<ObjectWithName<Blob>> getBlobFiles() {
        return blobFiles;
    }

    public @NotNull ArrayList<VcsTree> getVcsTreeFiles() {
        return vcsTreeFiles;
    }

    /**
     * Добавить в список файлов текущей директории еще один.
     * @param blob объект blob.
     * @param name имя файла.
     */
    public void addBlob(@NotNull Blob blob, @NotNull String name) {
        blobFiles.add(new ObjectWithName<>(blob, name));
    }

    /**
     * Добавить в список директорий текущей директории еще одну.
     * @param vcsTree директория, которую нужно добавить.
     */
    public void addChild(@NotNull VcsTree vcsTree) {
        vcsTreeFiles.add(vcsTree);
    }

    /**
     * Получение всех файлов, которые находятся в дереве.
     * @return множество всех объектов, содержащихся в дереве.
     */
    public @NotNull HashSet<ObjectWithName<Blob>> getAllFiles() {
        HashSet<ObjectWithName<Blob>> result = new HashSet<>();
        result.addAll(blobFiles);

        for (VcsTree subVcsTree : vcsTreeFiles) {
            result.addAll(subVcsTree.getAllFiles());
        }

        return result;
    }

    /**
     * Слияние с другим деревом.
     * @param vcsTree дерево, которое вливается в текущее.
     */
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

    @Override
    public int compareTo(@NotNull VcsTree tree) {
        return prefix.compareTo(tree.getPrefix());
    }
}