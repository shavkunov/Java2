package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public interface Datastore {
    void writeHead(@NotNull String revision) throws IOException;
    void writeContent(@NotNull Path pathToFile, @NotNull byte[] content) throws IOException;
    void restoreFile(@NotNull Path pathToFile, @NotNull String fileHash) throws IOException;
    void storeObject(@NotNull VcsObjectWithHash object) throws IOException;
    @NotNull String getHead() throws IOException;
    void storeReference(@NotNull String name, @NotNull String commitHash) throws IOException;
    @NotNull Commit getCommitByHash(@NotNull String commitHash) throws IOException, ClassNotFoundException;
    @NotNull VcsTree getTreeByHash(@NotNull String treeHash) throws IOException, ClassNotFoundException;
    @NotNull String getReferenceCommitHash(@NotNull String referenceName) throws IOException;
    @NotNull FilesTree getFilesTree(@NotNull HashSet<String> excludeFiles) throws NoRootDirectoryExistsException;
    void initResources(@NotNull Path rootDirectory) throws IOException;
    void createNewBranch(@NotNull String branchName, @NotNull String commitHash)
                                                        throws BranchAlreadyExistsException, IOException;
    void deleteBranch(@NotNull String branchName) throws IOException;
    boolean isBranchExists(@NotNull String branchName);
    boolean isCommitExists(@NotNull String commitHash);
    @NotNull Path getRootDirectory();
    void updateIndex(@NotNull Map<Path, String> index) throws IOException;
    @NotNull Map<Path, String> readIndex() throws IOException;
    void addTree(@NotNull VcsTree tree, @NotNull Path rootPath) throws IOException;
    void clean(@NotNull HashSet<String> untrackedFiles) throws ClassNotFoundException, NotRegularFileException,
                                                               NoRootDirectoryExistsException, IOException;
}