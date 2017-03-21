package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;

interface VcsObject {
    Path getPathToObject(Repository repository);
}