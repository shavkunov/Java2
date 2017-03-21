package ru.spbau.shavkunov.vcs;

public abstract class VcsObjectWithHash implements VcsObject {
    protected String hash;

    public String getHash() {
        return hash;
    }
}
