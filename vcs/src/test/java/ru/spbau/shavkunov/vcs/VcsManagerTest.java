package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static ru.spbau.shavkunov.vcs.Constants.MERGE_MESSAGE;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class VcsManagerTest {
    private VcsManager manager;
    private Repository repository;

    @Before
    public void setUp() throws IOException, NoRepositoryException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
        rootPath.resolve("test").toFile().delete();
        rootPath.resolve("test1").toFile().delete();
        rootPath.resolve("test2").toFile().delete();

        Repository.initRepository(rootPath);
        repository = Repository.getRepository(rootPath);
        manager = new VcsManager(repository);

        createTempDirectories();
    }

    @Test
    public void addFileTest() throws Exception, NotRegularFileException {
        String hash = manager.addFile(pathToFile);
        Path pathToFileInVcs = repository.getObjectsPath().resolve(hash);
        assertTrue(pathToFileInVcs.toFile().exists());
        assertEquals(Arrays.toString(Files.readAllBytes(pathToFile)),
                     Arrays.toString(Files.readAllBytes(pathToFileInVcs)));

        String firstIndexLine = Repository.getFirstLine(repository.getIndexPath());
        String pathWithHash[] = firstIndexLine.split(" ");
        Path pathToFileInIndex = Paths.get(pathWithHash[0]);
        String hashFileInIndex = pathWithHash[1];
        assertEquals(hash, hashFileInIndex);
        assertEquals(pathToFile, pathToFileInIndex);
    }

    @Test
    public void removeFileTest() throws Exception, NotRegularFileException {
        addFileTest();
        manager.removeFile(pathToFile);
        String firstIndexLine = Repository.getFirstLine(repository.getIndexPath());
        assertEquals("", firstIndexLine);
    }

    @Test
    public void commitChangesTest() throws Exception, NotRegularFileException {
        addFileTest();
        String author = "me";
        String message = "test commit";
        manager.commitChanges(author, message);

        String commitHash = Repository.getFirstLine(repository.getReferencesPath().resolve("master"));
        assertTrue(repository.getObjectsPath().resolve(commitHash).toFile().exists());
        Commit commit = new Commit(commitHash, repository);

        assertEquals(author, commit.getAuthor());
        assertEquals(message, commit.getMessage());

        VcsTree vcsTree = new VcsTree(commit.getTreeHash(), repository);
        assertTrue(vcsTree.isFileExists(pathToFile));
    }

    @Test
    public void checkoutToNewBranchTest() throws Exception, NotRegularFileException {
        addFileTest();
        commitChangesTest();
        String branchName = "test";
        manager.checkoutToNewBranch(branchName);

        assertTrue(repository.getReferencesPath().resolve(branchName).toFile().exists());
        assertEquals(Repository.getFirstLine(repository.getReferencesPath().resolve("master")),
                     Repository.getFirstLine(repository.getReferencesPath().resolve(branchName)));
    }

    @Test
    public void checkoutTest() throws Exception, NotRegularFileException, NoRevisionExistsException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        VcsTree masterVcsTree = manager.createTreeFromIndex();

        manager.checkoutToNewBranch("test");
        manager.addFile(rootPath.resolve("test").resolve("test3"));
        manager.commitChanges("me", "test 1 commit");
        manager.checkout("master");
        VcsTree testVcsTree = manager.createTreeFromIndex();

        assertEquals(masterVcsTree.getHash(), testVcsTree.getHash());
    }

    @Test(expected = CannotDeleteCurrentBranchException.class)
    public void deleteCurrentBranchTest() throws Exception, NotRegularFileException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        manager.checkoutToNewBranch("test");
        manager.deleteBranch("test");
    }

    @Test
    public void deleteBranchTest() throws Exception, NotRegularFileException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        manager.checkoutToNewBranch("test");
        manager.deleteBranch("master");

        assertFalse(repository.getReferencesPath().resolve("master").toFile().exists());
    }

    @Test
    public void getLogTest() throws Exception, NotRegularFileException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        manager.addFile(rootPath.resolve("test").resolve("test3"));
        manager.commitChanges("me", "master 2 commit");

        VcsLog log = manager.getLog();
        assertEquals(2, log.getCommits().size());
        assertEquals(log.getCommits().get(0).getMessage(), "master 1 commit");
        assertEquals(log.getCommits().get(1).getMessage(), "master 2 commit");
    }

    @Test
    public void mergeTest() throws Exception, NotRegularFileException, NoRevisionExistsException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        manager.checkoutToNewBranch("feature");
        manager.addFile(rootPath.resolve("test").resolve("test3"));
        manager.commitChanges("me", "implemented feature");
        String featureCommitHash = Repository.getFirstLine(repository.getReferencesPath().resolve("feature"));
        TimeUnit.SECONDS.sleep(2);
        manager.checkout("master");
        manager.merge("feature");

        VcsLog log = manager.getLog();
        assertEquals(MERGE_MESSAGE + featureCommitHash, log.getCommits().get(1).getMessage());
    }

    @Test
    public void statusTest() throws IOException, NotRegularFileException,
                                    NoRootDirectoryExistsException, ClassNotFoundException {
        manager.addFile(rootPath.resolve("test1"));
        manager.commitChanges("me", "master 1 commit");

        manager.addFile(rootPath.resolve("test2"));
        manager.removeFile(rootPath.resolve("test1"));

        assertEquals(1, manager.getStagedFiles().size());
        assertEquals("test2", manager.getStagedFiles().get(0));

        assertEquals(1, manager.getDeletedFiles().size());
        assertEquals("test1", manager.getDeletedFiles().get(0));
    }

    @Test
    public void resetTest() throws IOException, NotRegularFileException, ClassNotFoundException {
        Path pathToTest = rootPath.resolve("test1");
        manager.addFile(pathToTest);
        manager.commitChanges("me", "master 1 commit");
        Files.write(pathToTest, "another text".getBytes());

        assertEquals("test1", manager.getModifiedFiles().get(0));
        manager.reset(pathToTest);

        assertEquals("text1", Repository.getFirstLine(pathToTest));
    }

    @Test
    public void cleanTest() {
        // TODO
        // careful with untracked files!
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());

        FileUtils.deleteDirectory(rootPath.resolve("test").toFile());
        rootPath.resolve("test1").toFile().delete();
        rootPath.resolve("test2").toFile().delete();
    }

    private void createTempDirectories() throws IOException {
        Files.write(rootPath.resolve("test1"), "text1".getBytes());
        Files.write(rootPath.resolve("test2"), "text2".getBytes());
        rootPath.resolve("test").toFile().mkdir();
        Files.write(rootPath.resolve("test").resolve("test3"), "test3".getBytes());
    }
}