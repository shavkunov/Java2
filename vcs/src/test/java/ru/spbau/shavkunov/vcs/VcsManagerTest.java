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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToVcs;

public class VcsManagerTest {
    private Path rootPath = Paths.get("");
    private Path pathToFile = pathToVcs.resolve("VcsObject.java");
    private VcsManager manager;
    private Repository repository;

    @Before
    public void setUp() throws IOException, NoRepositoryException {
        Repository.initRepository(rootPath);
        repository = Repository.getRepository(rootPath);
        manager = new VcsManager(repository);
    }

    @Test
    public void addFile() throws Exception, NotRegularFileException {
        pathToFile = pathToVcs.resolve("VcsObject.java");
        String hash = manager.addFile(pathToFile);
        Path pathToFileInVcs = repository.getObjectsPath().resolve(hash);
        assertTrue(pathToFileInVcs.toFile().exists());
        assertEquals(Arrays.toString(Files.readAllBytes(pathToFile)),
                     Arrays.toString(Files.readAllBytes(pathToFileInVcs)));

        BufferedReader reader = new BufferedReader(new FileReader(repository.getIndexPath().toFile()));
        String line = reader.readLine();
        String pathWithHash[] = line.split(" ");
        Path pathToFileInIndex = Paths.get(pathWithHash[0]);
        String hashFileInIndex = pathWithHash[1];
        assertEquals(hash, hashFileInIndex);
        assertEquals(pathToFile, pathToFileInIndex);
    }

    @Test
    public void removeFile() throws Exception, NotRegularFileException {
        addFile();
        manager.removeFile(pathToFile);


    }

    @Test
    public void commitChanges() throws Exception {

    }

    @Test
    public void checkoutToNewBranch() throws Exception {

    }

    @Test
    public void checkout() throws Exception {

    }

    @Test
    public void deleteBranch() throws Exception {

    }

    @Test
    public void getLog() throws Exception {

    }

    @Test
    public void merge() throws Exception {

    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
    }
}