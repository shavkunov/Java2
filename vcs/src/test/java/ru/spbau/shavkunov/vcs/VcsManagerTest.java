package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.data.Filesystem;
import ru.spbau.shavkunov.vcs.exceptions.*;
import ru.spbau.shavkunov.vcs.trees.VcsTree;
import ru.spbau.shavkunov.vcs.utils.Utils;
import ru.spbau.shavkunov.vcs.utils.VcsLog;
import ru.spbau.shavkunov.vcs.primitives.Commit;
import ru.spbau.shavkunov.vcs.primitives.Reference;
import ru.spbau.shavkunov.vcs.primitives.Repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static ru.spbau.shavkunov.vcs.utils.Constants.*;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class VcsManagerTest {
    private VcsManager manager;
    private Repository repository;
    private Filesystem filesystem;

    @Before
    public void setUp() throws IOException, NoRepositoryException, RepositoryAlreadyExistsException {
        deleteTmpFiles();

        Repository.initResources(rootPath);
        manager = new VcsManager(rootPath);

        filesystem = new Filesystem(rootPath);
        repository = new Repository(filesystem);

        createTempDirectories();
    }

    @Test
    public void addFileTest() throws Exception, NotRegularFileException {
        String hash = manager.addFile(pathToFile);
        Path pathToFileInVcs = rootPath.resolve(VCS_FOLDER).resolve(OBJECTS_FOLDER).resolve(hash);
        assertTrue(pathToFileInVcs.toFile().exists());
        assertEquals(Arrays.toString(Files.readAllBytes(pathToFile)),
                     Arrays.toString(Files.readAllBytes(pathToFileInVcs)));

        String firstIndexLine = Utils.getFirstLine(filesystem.getIndexPath());
        String pathWithHash[] = firstIndexLine.split(" ");
        Path pathToFileInIndex = Paths.get(pathWithHash[0]);
        String hashFileInIndex = pathWithHash[1];
        assertEquals(hash, hashFileInIndex);
        assertEquals(pathToFile, pathToFileInIndex);
    }

    @Test(expected = NotRegularFileException.class)
    public void addNotFileTest() throws IOException, NotRegularFileException {
        manager.addFile(rootPath.resolve("test"));
    }

    @Test
    public void removeFileTest() throws Exception, NotRegularFileException {
        addFileTest();
        manager.removeFile(pathToFile);
        String firstIndexLine = Utils.getFirstLine(filesystem.getIndexPath());
        assertEquals("", firstIndexLine);
    }

    @Test
    public void commitChangesTest() throws Exception, NotRegularFileException {
        manager.addFile(pathToFile);

        String author = "me";
        String message = "test commit";
        manager.commitChanges(author, message);

        String commitHash = Utils.getFirstLine(filesystem.getReferencesPath().resolve("master"));
        assertTrue(filesystem.getObjectsPath().resolve(commitHash).toFile().exists());
        Commit commit = repository.getCommit(commitHash);

        assertEquals(author, commit.getAuthor());
        assertEquals(message, commit.getMessage());

        VcsTree vcsTree = repository.getTree(commit.getTreeHash());
        assertTrue(vcsTree.isFileExists(pathToFile));
    }

    @Test
    public void checkoutToNewBranchTest() throws Exception, NotRegularFileException {
        addFileTest();
        commitChangesTest();
        String branchName = "test";
        manager.checkoutToNewBranch(branchName);

        assertTrue(filesystem.getReferencesPath().resolve(branchName).toFile().exists());
        assertEquals(Utils.getFirstLine(filesystem.getReferencesPath().resolve("master")),
                     Utils.getFirstLine(filesystem.getReferencesPath().resolve(branchName)));
    }

    @Test
    public void checkoutTest() throws Exception, NotRegularFileException, NoRevisionExistsException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        repository = new Repository(rootPath);
        VcsTree masterVcsTree = repository.createTreeFromIndex();

        Reference currentReference = repository.getReference();
        Commit currentCommit = repository.getCommit(currentReference.getCommitHash());
        assertEquals(currentCommit.getTreeHash(),
                     masterVcsTree.getHash());

        manager.checkoutToNewBranch("test");
        manager.addFile(rootPath.resolve("test").resolve("test3"));
        manager.commitChanges("me", "test 1 commit");
        manager.checkout("master");
        VcsTree testVcsTree = repository.createTreeFromIndex();

        assertEquals(masterVcsTree.getHash(), testVcsTree.getHash());
    }

    @Test
    public void doubleCheckoutTest() throws IOException, NotRegularFileException,
                                            NoRepositoryException, BranchAlreadyExistsException,
                                            NoRevisionExistsException, ClassNotFoundException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        repository = new Repository(rootPath);

        manager.checkoutToNewBranch("test_branch");
        manager.addFile(rootPath.resolve("test").resolve("test3"));
        manager.commitChanges("me", "test 1 commit");
        VcsTree testVcsTree = repository.createTreeFromIndex();

        manager.checkout("master");
        manager.checkout("test_branch");
        VcsTree secondTestTree = repository.createTreeFromIndex();

        assertEquals(testVcsTree.getHash(), secondTestTree.getHash());
    }

    @Test(expected = BranchAlreadyExistsException.class)
    public void createNewBranchTest() throws IOException, BranchAlreadyExistsException {
        manager.checkoutToNewBranch("new_branch_name");
        manager.checkoutToNewBranch("master");
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

        assertFalse(filesystem.getReferencesPath().resolve("master").toFile().exists());
    }

    @Test(expected = NoRevisionExistsException.class)
    public void sameDeleteBranchTest() throws CannotDeleteCurrentBranchException, NoBranchExistsException,
                                              IOException, NoRevisionExistsException, ClassNotFoundException,
                                              NotRegularFileException, BranchAlreadyExistsException {
        manager.addFile(rootPath.resolve("test1"));
        manager.addFile(rootPath.resolve("test2"));
        manager.commitChanges("me", "master 1 commit");
        manager.checkoutToNewBranch("test");
        manager.deleteBranch("master");
        manager.checkout("master");
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
        String featureCommitHash = Utils.getFirstLine(filesystem.getReferencesPath().resolve("feature"));
        TimeUnit.SECONDS.sleep(1);
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

        assertEquals("text1", Utils.getFirstLine(pathToTest));
    }

    @Test
    public void cleanTest() throws IOException, NoRepositoryException,
                                   NotRegularFileException, NoRootDirectoryExistsException,
                                   ClassNotFoundException, RepositoryAlreadyExistsException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
        Path newRootPath = rootPath.resolve("test");

        Repository.initResources(newRootPath);
        manager = new VcsManager(newRootPath);

        manager.addFile(newRootPath.resolve("test3"));
        manager.addFile(newRootPath.resolve("test4"));
        manager.commitChanges("me", "master first commit");
        manager.clean();

        assertFalse(newRootPath.resolve("test5").toFile().exists());
    }

    @After
    public void tearDown() throws IOException {
        //deleteTmpFiles();
    }

    public void deleteTmpFiles() throws IOException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());

        FileUtils.deleteDirectory(rootPath.resolve("test").toFile());
        rootPath.resolve("test1").toFile().delete();
        rootPath.resolve("test2").toFile().delete();
    }

    public void createTempDirectories() throws IOException {
        Files.write(rootPath.resolve("test1"), "text1".getBytes());
        Files.write(rootPath.resolve("test2"), "text2".getBytes());
        rootPath.resolve("test").toFile().mkdir();
        Files.write(rootPath.resolve("test").resolve("test3"), "test3".getBytes());
        Files.write(rootPath.resolve("test").resolve("test4"), "test4".getBytes());
        Files.write(rootPath.resolve("test").resolve("test5"), "test5".getBytes());
    }

    private void printIndexFile() {
        File index = Paths.get(".").resolve(VCS_FOLDER).resolve("index").toFile();

        try (BufferedReader br = new BufferedReader(new FileReader(index))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
    }
}