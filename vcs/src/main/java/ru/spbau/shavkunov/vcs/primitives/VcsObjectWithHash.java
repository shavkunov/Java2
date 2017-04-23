package ru.spbau.shavkunov.vcs.primitives;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;

/**
 * Класс, описывающий логику взаимодействия с хешом объекта, если такой у него присутствует.
 */
public abstract class VcsObjectWithHash implements Serializable {
    /**
     * Хеш объекта.
     */
    protected @Nullable String hash;

    /**
     * Получение хеша у объекта.
     * @return хеш объекта.
     */
    public @Nullable String getHash() {
        return hash;
    }

    /**
     * Получение содержимого объекта в виде массива байтов.
     * @return содержимого объекта.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public abstract @NotNull byte[] getContent() throws IOException;
}