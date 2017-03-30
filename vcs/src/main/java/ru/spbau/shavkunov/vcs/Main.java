package ru.spbau.shavkunov.vcs;

import org.apache.commons.cli.*;
import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRevisionExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.spbau.shavkunov.vcs.Constants.USERNAME;

public class Main {
    private static final String INIT_COMMAND = "init";
    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "rm";
    private static final String RESET_COMMAND = "reset";
    private static final String COMMIT_COMMAND = "commit";
    private static final String NEW_BRANCH_COMMAND = "new_branch";
    private static final String CHECKOUT_COMMAND = "checkout";
    private static final String LOG_COMMAND = "log";
    private static final String MERGE_COMMAND = "merge";
    private static final String STATUS_COMMAND = "status";
    private static final String CLEAN_COMMAND = "clean";
    private static Path rootPath = Paths.get(".");

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(initOption());
        options.addOption(addFileOption());
        options.addOption(rmFileOption());
        options.addOption(resetFileOption());
        options.addOption(commitOption());
        options.addOption(newBranchOption());
        options.addOption(checkoutOption());
        options.addOption(logOption());
        options.addOption(mergeOption());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse( options, args);
            if (cmd.hasOption(INIT_COMMAND)) {
                handleInit(cmd);
            }

            if (cmd.hasOption(ADD_COMMAND)) {
                handleAdd(cmd);
            }

            if (cmd.hasOption(REMOVE_COMMAND)) {
                handleRemove(cmd);
            }

            if (cmd.hasOption(RESET_COMMAND)) {
                handleReset(cmd);
            }

            if (cmd.hasOption(COMMIT_COMMAND)) {
                handleCommit(cmd);
            }

            if (cmd.hasOption(NEW_BRANCH_COMMAND)) {
                handleCreateNewBranch(cmd);
            }

            if (cmd.hasOption(CHECKOUT_COMMAND)) {
                handleCheckout(cmd);
            }

            if (cmd.hasOption(LOG_COMMAND)) {
                handleLog(cmd);
            }

            if (cmd.hasOption(MERGE_COMMAND)) {
                handleMerge(cmd);
            }

            if (cmd.hasOption(STATUS_COMMAND)) {
                handleStatus(cmd);
            }

            if (cmd.hasOption(CLEAN_COMMAND)) {
                handleClean(cmd);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static Option initOption() {
        Option initOption = new Option(INIT_COMMAND, true, "Init repository");
        initOption.setArgs(1);
        initOption.setOptionalArg(true);
        return initOption;
    }

    private static void handleInit(CommandLine cmd) {
        String[] initArgs = cmd.getOptionValues(INIT_COMMAND);
        if (initArgs.length == 1) {
            rootPath = Paths.get(initArgs[1]).normalize();
        }

        try {
            Repository.initRepository(rootPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Option addFileOption() {
        return new Option(ADD_COMMAND, true, "add file(-s)");
    }

    private static void handleAdd(CommandLine cmd) {
        String[] addArgs = cmd.getOptionValues(ADD_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : addArgs) {
                manager.addFile(Paths.get(file).normalize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        }
    }

    private static Option rmFileOption() {
        Option deleteOption = new Option(REMOVE_COMMAND, true, "remove file(-s)");
        return deleteOption;
    }

    private static void handleRemove(CommandLine cmd) {
        String[] removeArgs = cmd.getOptionValues(ADD_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : removeArgs) {
                manager.removeFile(Paths.get(file).normalize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        }
    }

    private static Option resetFileOption() {
        Option resetOption = new Option(RESET_COMMAND, true, "reset file(-s)");
        resetOption.setArgs(1);
        return resetOption;
    }

    private static void handleReset(CommandLine cmd) {
        String[] resetArgs = cmd.getOptionValues(RESET_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : resetArgs) {
                manager.reset(Paths.get(file).normalize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Option commitOption() {
        Option commitOption = new Option(COMMIT_COMMAND, true, "record changes in repository");
        commitOption.setArgs(1);
        return commitOption;
    }

    private static void handleCommit(CommandLine cmd) {
        String[] commitArgs = cmd.getOptionValues(COMMIT_COMMAND);
        String message = commitArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.commitChanges(USERNAME, message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        }
    }

    private static Option newBranchOption() {
        Option branchOption = new Option(NEW_BRANCH_COMMAND, true, "creating new branch");
        branchOption.setArgs(1);
        return branchOption;
    }

    private static void handleCreateNewBranch(CommandLine cmd) {
        String[] branchArgs = cmd.getOptionValues(NEW_BRANCH_COMMAND);
        String newBranchName = branchArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.checkoutToNewBranch(newBranchName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BranchAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    private static Option checkoutOption() {
        Option checkoutOption = new Option(CHECKOUT_COMMAND, true, "checkout to existing branch or to hash of commit");
        checkoutOption.setArgs(1);
        return checkoutOption;
    }

    private static void handleCheckout(CommandLine cmd) {
        String[] checkoutArgs = cmd.getOptionValues(CHECKOUT_COMMAND);
        String revision = checkoutArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.checkout(revision);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoRevisionExistsException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Option logOption() {
        return new Option(LOG_COMMAND, false, "get current log");
    }

    private static void handleLog(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.getLog().printLog();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Option mergeOption() {
        Option mergeOption = new Option(MERGE_COMMAND, true, "merge that branch to current");
        mergeOption.setArgs(1);
        return mergeOption;
    }

    private static void handleMerge(CommandLine cmd) {
        String[] mergeArgs = cmd.getOptionValues(MERGE_COMMAND);
        String branchName = mergeArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.merge(branchName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Option statusOption() {
        return new Option(STATUS_COMMAND, false, "get description of current state");
    }

    private static void handleStatus(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.status();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoRootDirectoryExistsException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Option cleanOption() {
        return new Option(CLEAN_COMMAND, false, "remove untracked files from repository");
    }

    private static void handleClean(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.clean();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoRootDirectoryExistsException e) {
            e.printStackTrace();
        } catch (NotRegularFileException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}