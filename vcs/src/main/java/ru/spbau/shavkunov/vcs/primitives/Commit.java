package ru.spbau.shavkunov.vcs.primitives;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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

    /**
     * Создание коммита и сохранение его в репозиторий.
     * @param author автор коммита.
     * @param message сообщение коммита.
     * @param treeHash хеш дерева репозитория.
     * @param parentCommits коммиты предки.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public Commit(@NotNull String author, @NotNull String message, @NotNull String treeHash,
                  @NotNull ArrayList<String> parentCommits)
                 throws IOException {
        this.date = new Date();
        this.author = author;
        this.message = message;
        this.treeHash = treeHash;
        this.parentCommits = parentCommits;

        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
    }

    /**
     * Получение коммита по хешу.
     * @param content сериализованная информация коммита.
     * @param commitHash хеш коммита.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws ClassNotFoundException исключение, если не удалось интерпретировать данные(хеш не коммита)
     */
    @SuppressWarnings("unchecked")
    public Commit(@NotNull byte[] content, @NotNull String commitHash) throws IOException, ClassNotFoundException {
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

    /**
     * Получение даты коммита.
     * @return возвращает дату коммита.
     */
    public @NotNull Date getDate() {
        return date;
    }

    /**
     * Получение автора коммита.
     * @return возвращает автора коммита.
     */
    public @NotNull String getAuthor() {
        return author;
    }

    /**
     * Получение сообщения коммита.
     * @return возвращает сообщение коммита.
     */
    public @NotNull String getMessage() {
        return message;
    }

    /**
     * Получение хеша дерева коммита.
     * @return возвращает хеш дерева коммита.
     */
    public @NotNull String getTreeHash() {
        return treeHash;
    }

    @Override
    public @NotNull byte[] getContent() throws IOException {
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

    /**
     * Получение коммитов предков текущего коммита.
     * @return возвращает предков коммита.
     */
    public @NotNull List<String> getParentCommits() {
        return parentCommits;
    }
}