package ru.spbau.shavkunov.vcs;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class TreeTest {
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
        Tree tree = manager.createTreeFromIndex();
        assertNotNull(tree.getHash());
    }

    @Test
    public void checkFileInTree() throws IOException, NotRegularFileException {
        Tree tree = manager.createTreeFromIndex();
        tree.isFileExists(pathToFile);
    }

    @After
    public void tearDown() throws IOException {
        test.tearDown();
    }
}