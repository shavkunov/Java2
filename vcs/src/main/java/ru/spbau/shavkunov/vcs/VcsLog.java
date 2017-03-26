package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Класс, отвечающий за логику вывода сообщений коммитов.
 */
public class VcsLog {
    /**
     * Список коммитов, сообщение о которых нужно вывести.
     */
    private @NotNull ArrayList<Commit> commits;

    public VcsLog(@NotNull ArrayList<Commit> commits) {
        commits.sort(Comparator.comparing(Commit::getDate));
        this.commits = commits;
    }

    /**
     * Печать лога коммитов.
     */
    public void printLog() {
        for (Commit commit : commits) {
            printCommitLog(commit);
        }
    }

    /**
     * Печать сообщения данного коммита.
     * @param commit сообщение этого коммита нужно распечатать.
     */
    private void printCommitLog(@NotNull Commit commit) {
        String message = "Date : " + commit.getDate().toString() + "\n" +
                         "Author : " + commit.getAuthor() + "\n" +
                         "Commit message : " + commit.getMessage() + "\n" +
                         "Commit hash : " + commit.getHash() + "\n";

        System.out.println(message);
    }

    public ArrayList<Commit> getCommits() {
        return commits;
    }
}