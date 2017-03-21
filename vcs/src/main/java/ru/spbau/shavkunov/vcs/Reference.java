package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;

import static ru.spbau.shavkunov.vcs.Constants.REFERENCES_FOLDER;

public class Reference implements VcsObject {
    private String name; // название файла в refs
    private String commitHash; // содержимое этого файла

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(REFERENCES_FOLDER).resolve(name);
    }


}
