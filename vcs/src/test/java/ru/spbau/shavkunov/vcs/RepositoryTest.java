package ru.spbau.shavkunov.vcs;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.shavkunov.vcs.exceptions.*;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static ru.spbau.shavkunov.vcs.Constants.DEFAULT_BRANCH_NAME;
import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;
import static ru.spbau.shavkunov.vcs.TestConstants.pathToFile;
import static ru.spbau.shavkunov.vcs.TestConstants.rootPath;

public class RepositoryTest {
    @Before
    public void setUp() throws IOException {
        FileUtils.deleteDirectory(rootPath.resolve(VCS_FOLDER).toFile());
    }

    @Test
    public void initRepositoryTest() throws Exception {
        Repository.initRepository(rootPath);
        Repository repository = Repository.getRepository(rootPath);
        assertThat(repository.getRootDirectory(), is(rootPath));
        assertEquals(4, repository.getRootDirectory().resolve(VCS_FOLDER).toFile().listFiles().length);
    }

    @Test
    public void branchManageTest() throws IOException, NoRepositoryException, NotRegularFileException,
            BranchAlreadyExistsException, CannotDeleteCurrentBranchException, NoBranchExistsException {
        Repository.initRepository(rootPath);
        Repository repository = Repository.getRepository(rootPath);
        assertTrue(repository.isBranchExists(DEFAULT_BRANCH_NAME));
        VcsManager manager = new VcsManager(repository);

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
    }
}