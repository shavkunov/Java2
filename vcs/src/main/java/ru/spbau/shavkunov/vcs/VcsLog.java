package ru.spbau.shavkunov.vcs;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Класс, отвечающий за логику вывода сообщений коммитов.
 */
public class VcsLog {
    private ArrayList<Commit> commits;

    public VcsLog(ArrayList<Commit> commits) {
        commits.sort(Comparator.comparing(Commit::getDate));
        this.commits = commits;
    }

    public void printLog() {
        for (Commit commit : commits) {
            printCommitLog(commit);
        }
    }

    private void printCommitLog(Commit commit) {
        String message = "Date : " + commit.getDate().toString() + "\n" +
                         "Author : " + commit.getAuthor() + "\n" +
                         "Commit message : " + commit.getMessage() + "\n" +
                         "Commit hash : " + commit.getHash() + "\n";

        System.out.println(message);
    }
}