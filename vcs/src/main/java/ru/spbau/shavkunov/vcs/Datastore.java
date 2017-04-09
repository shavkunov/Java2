package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public interface Datastore {
    void writeContent(@NotNull Path pathToFile, byte[] content);
    void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash);
    void storeObject(VcsObjectWithHash object);
    FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles);
    void initResources() throws IOException;
    @NotNull Path getRootDirectory();
    @NotNull Path getIndexPath();
    @NotNull Path getObjectsPath();
    @NotNull Path getReferencesPath();
    @NotNull Path getHead();
    void updateIndex(@NotNull Map<Path, String> index) throws IOException;
    @NotNull Map<Path, String> readIndex() throws IOException;
    void addTree(@NotNull VcsTree tree, @NotNull Path rootPath) throws IOException;
    void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException, NoRootDirectoryExistsException, IOException;
}