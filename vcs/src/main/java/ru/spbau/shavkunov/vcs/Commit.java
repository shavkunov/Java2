package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Реализация класс, который представляет собой некоторое состояние репозитория.
 */
public class Commit extends VcsObjectWithHash {
    /**
     * Дата сделанного коммита.
     */
    private @NotNull Date date;

    /**
     * Автор коммита.
     */
    private @NotNull String author;

    /**
     * Сообщение коммита.
     */
    private @NotNull String message;

    /**
     * Хеш дерева, которое хранит в себе структуру файлов и папок.
     */
    private @NotNull String treeHash;

    /**
     * Список хешей коммитов, порождающих данный.
     */
    private @NotNull ArrayList<String> parentCommits;

    public Commit(@NotNull String author, @NotNull String message, @NotNull String treeHash,
                  @NotNull ArrayList<String> parentCommits, @NotNull Repository repository)
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

    public @NotNull Date getDate() {
        return date;
    }

    public @NotNull String getAuthor() {
        return author;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public @NotNull String getTreeHash() {
        return treeHash;
    }

    public Commit(@NotNull String commitHash, @NotNull Repository repository) throws IOException, ClassNotFoundException {
        byte[] content = Files.readAllBytes(repository.getObjectsPath().resolve(commitHash));

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
             ObjectInputStream input = new ObjectInputStream(byteArrayInputStream)) {

            date = (Date) input.readObject();
            author = (String) input.readObject();
            message = (String) input.readObject();
            treeHash = (String) input.readObject();
            parentCommits = (ArrayList<String>) input.readObject();
        }

        hash = commitHash;
    }

    private @NotNull byte[] getContent() throws IOException {
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

    public @NotNull List<String> getParentCommits() {
        return parentCommits;
    }

    @Override
    public @NotNull Path getPathToObject(@NotNull Repository repository) {
        return repository.getObjectsPath().resolve(hash);
    }
}