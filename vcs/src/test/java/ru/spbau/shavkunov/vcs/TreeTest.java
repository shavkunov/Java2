package ru.spbau.shavkunov.vcs;


import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import static org.junit.Assert.assertNotNull;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class TreeTest {
    @Test
    public void checkCreatingTreeFromIndex() throws Exception, NotRegularFileException {
        VcsManagerTest test = new VcsManagerTest();
        test.setUp();
        test.addFile();

        Repository repository = Repository.getRepository(rootPath);
        VcsManager manager = new VcsManager(repository);
        Tree tree = Tree.createTreeFromIndex(manager.getIndex(), repository);
        tree.printTree();

        assertNotNull(tree.getHash());
        test.tearDown();
    }
}