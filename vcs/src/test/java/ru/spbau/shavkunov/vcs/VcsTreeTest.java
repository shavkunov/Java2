package ru.spbau.shavkunov.vcs;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class VcsTreeTest {
    private VcsManager manager;
    private VcsManagerTest test;

    @Before
    public void setUp() throws Exception, NotRegularFileException {
        test = new VcsManagerTest();
        test.setUp();
        test.addFileTest();
        Repository repository = Repository.getRepository(rootPath);
        manager = new VcsManager(repository);
    }

    @Test
    public void checkCreatingTreeFromIndex() throws Exception, NotRegularFileException {
        VcsTree vcsTree = manager.createTreeFromIndex();
        assertNotNull(vcsTree.getHash());
    }

    @Test
    public void checkFileInTree() throws IOException, NotRegularFileException {
        VcsTree vcsTree = manager.createTreeFromIndex();
        assertTrue(vcsTree.isFileExists(pathToFile));
        assertTrue(vcsTree.getAllFiles().contains(pathToFile));
    }

    @After
    public void tearDown() throws IOException {
        test.tearDown();
    }
}