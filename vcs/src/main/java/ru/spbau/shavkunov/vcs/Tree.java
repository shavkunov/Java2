package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

interface Tree {
    void printTree(int spaces);
    boolean isFileExists(Path pathToFile);


    static @NotNull String multiply(@NotNull String sample, int amount) {
        String copy = "";

        for (int i = 0; i < amount; i++) {
            copy += sample;
        }

        return copy;
    }
}
