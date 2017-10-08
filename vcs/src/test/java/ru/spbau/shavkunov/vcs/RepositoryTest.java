package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.*;
import ru.spbau.shavkunov.vcs.primitives.Repository;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static ru.spbau.shavkunov.vcs.utils.Constants.DEFAULT_BRANCH_NAME;
import static ru.spbau.shavkunov.vcs.utils.Constants.VCS_FOLDER;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class RepositoryTest {
    @Before
    public void setUp() throws IOException, NoRepositoryException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
        new VcsManagerTest().createTempDirectories();
    }

    @Test
    public void initRepositoryTest() throws Exception {
        Repository.initResources(rootPath);
        Repository repository = new Repository(rootPath);
        assertThat(repository.getRootDirectory(), is(rootPath.resolve(VCS_FOLDER)));
        assertEquals(4, repository.getRootDirectory().toFile().listFiles().length);

        assertEquals("master", repository.getReference().getName());
    }

    @Test(expected = RepositoryAlreadyExistsException.class)
    public void initRepositoryAgainTest() throws IOException, RepositoryAlreadyExistsException {
        Repository.initResources(rootPath);
        Repository.initResources(rootPath);
    }

    @Test
    public void branchManageTest() throws IOException, NoRepositoryException, NotRegularFileException,
                                          BranchAlreadyExistsException, CannotDeleteCurrentBranchException,
                                          NoBranchExistsException, RepositoryAlreadyExistsException {
        Repository.initResources(rootPath);
        VcsManager manager = new VcsManager(rootPath);
        Repository repository = new Repository(rootPath);
        assertTrue(repository.isBranchExists(DEFAULT_BRANCH_NAME));

        manager.addFile(pathToFile);
        manager.commitChanges("me", "test commit");
        String branchName = "testBranch";
        assertFalse(repository.isBranchExists(branchName));
        manager.checkoutToNewBranch(branchName);
        assertTrue(repository.isBranchExists(branchName));
        manager.deleteBranch(DEFAULT_BRANCH_NAME);
        assertFalse(repository.isBranchExists(DEFAULT_BRANCH_NAME));
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
        new VcsManagerTest().deleteTmpFiles();
    }
}