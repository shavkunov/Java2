package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.NoRepositoryException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.*;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class VcsManagerTest {
    private VcsManager manager;
    private Repository repository;

    @Before
    public void setUp() throws IOException, NoRepositoryException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
        Repository.initRepository(rootPath);
        repository = Repository.getRepository(rootPath);
        manager = new VcsManager(repository);
    }

    @Test
    public void addFileTest() throws Exception, NotRegularFileException {
        String hash = manager.addFile(pathToFile);
        Path pathToFileInVcs = repository.getObjectsPath().resolve(hash);
        assertTrue(pathToFileInVcs.toFile().exists());
        assertEquals(Arrays.toString(Files.readAllBytes(pathToFile)),
                     Arrays.toString(Files.readAllBytes(pathToFileInVcs)));

        String firstIndexLine = getFirstLine(repository.getIndexPath());
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
        String firstIndexLine = getFirstLine(repository.getIndexPath());
        assertNull(firstIndexLine);
    }

    @Test
    public void commitChangesTest() throws Exception, NotRegularFileException {
        addFileTest();
        String author = "me";
        String message = "test commit";
        manager.commitChanges(author, message);

        String commitHash = getFirstLine(repository.getReferencesPath().resolve("master"));
        assertTrue(repository.getObjectsPath().resolve(commitHash).toFile().exists());
        Commit commit = new Commit(commitHash, repository);

        assertEquals(author, commit.getAuthor());
        assertEquals(message, commit.getMessage());

        Tree tree = new Tree(commit.getTreeHash(), repository);
        assertTrue(tree.isFileExists(pathToFile));
    }

    @Test
    public void checkoutToNewBranchTest() throws Exception {

    }

    @Test
    public void checkoutTest() throws Exception {

    }

    @Test
    public void deleteBranchTest() throws Exception {

    }

    @Test
    public void getLogTest() throws Exception {

    }

    @Test
    public void mergeTest() throws Exception {

    }

    @After
    public void tearDown() throws IOException {
        //FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
    }

    private String getFirstLine(Path pathToFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));
        return reader.readLine();
    }
}