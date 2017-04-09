package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;

/**
 * Класс, описывающий логику взаимодействия с хешом объекта, если такой у него присутствует.
 */
public abstract class VcsObjectWithHash implements Serializable {
    /**
     * Хеш объекта.
     */
    protected @NotNull String hash;

    public @NotNull String getHash() {
        return hash;
    }

    abstract byte[] getContent() throws IOException;
}