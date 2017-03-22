package ru.spbau.shavkunov.vcs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static ru.spbau.shavkunov.vcs.Constants.REFERENCES_FOLDER;

public class Reference implements VcsObject {
    private String name; // название файла в refs
    private String commitHash; // содержимое этого файла

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(REFERENCES_FOLDER).resolve(name);
    }

    public Reference(Repository repository) throws Exception {
        name = repository.getCurrentBranchName();
        if (name == null) {
            throw new Exception("hash commit instead of branch name");
        }

        commitHash = Arrays.toString(Files.readAllBytes(repository.getReferencesPath().resolve(name)));
    }

    public Reference(String name, Repository repository) throws IOException {
        this.name = name;
        commitHash = Arrays.toString(Files.readAllBytes(repository.getReferencesPath().resolve(name)));
    }

    public String getName() {
        return name;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void refreshCommitHash(String newCommitHash, Repository repository) throws IOException {
        Files.write(getPathToObject(repository).resolve(name), newCommitHash.getBytes());
    }
}
