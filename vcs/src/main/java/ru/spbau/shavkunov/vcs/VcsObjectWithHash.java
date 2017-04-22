package ru.spbau.shavkunov.vcs;

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

    public @Nullable String getHash() {
        return hash;
    }

    abstract @NotNull byte[] getContent() throws IOException;
}