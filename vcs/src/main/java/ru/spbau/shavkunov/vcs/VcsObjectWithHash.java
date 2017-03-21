package ru.spbau.shavkunov.vcs;

import java.io.IOException;

public abstract class VcsObjectWithHash implements VcsObject {
    protected String hash;

    public String getHash() {
        return hash;
    }
    public abstract void saveToStorage(Repository repository) throws IOException;
}
