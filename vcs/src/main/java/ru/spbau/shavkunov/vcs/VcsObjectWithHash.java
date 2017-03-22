package ru.spbau.shavkunov.vcs;

/**
 * Класс, описывающий логику взаимодействия с хешом объекта, если такой у него присутствует.
 */
public abstract class VcsObjectWithHash implements VcsObject {
    protected String hash;

    public String getHash() {
        return hash;
    }
}
