package ru.spbau.shavkunov.vcs;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;
import ru.spbau.shavkunov.vcs.trees.VcsTree;
import ru.spbau.shavkunov.vcs.primitives.ObjectWithName;
import ru.spbau.shavkunov.vcs.primitives.Repository;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class VcsTreeTest {
    private VcsManager manager;
    private VcsManagerTest test;
    private Repository repository;

    @Before
    public void setUp() throws Exception, NotRegularFileException {
        test = new VcsManagerTest();
        test.setUp();
        test.addFileTest();
        manager = new VcsManager(rootPath);
        repository = new Repository(rootPath);
    }

    @Test
    public void checkCreatingTreeFromIndex() throws Exception, NotRegularFileException {
        VcsTree vcsTree = repository.createTreeFromIndex();
        assertNotNull(vcsTree.getHash());
    }

    @Test
    public void checkFileInTree() throws IOException, NotRegularFileException {
        VcsTree vcsTree = repository.createTreeFromIndex();
        assertTrue(vcsTree.isFileExists(pathToFile));
        assertTrue(vcsTree.getAllFiles()
                          .stream()
                          .map(ObjectWithName::getName)
                          .collect(Collectors.toSet())
                          .contains(pathToFile.toString()));
    }

    @After
    public void tearDown() throws IOException {
        test.tearDown();
    }
}