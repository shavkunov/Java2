package ru.spbau.shavkunov.vcs;

import org.apache.commons.cli.*;
import ru.spbau.shavkunov.vcs.exceptions.*;
import ru.spbau.shavkunov.vcs.primitives.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.spbau.shavkunov.vcs.utils.Constants.USERNAME;

/**
 * Класс, полностью отвечающий за парсинг комманд пользователя.
 */
public class Main {
    private static final String INIT_COMMAND = "init";
    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "rm";
    private static final String RESET_COMMAND = "reset";
    private static final String COMMIT_COMMAND = "commit";
    private static final String NEW_BRANCH_COMMAND = "new_branch";
    private static final String CHECKOUT_COMMAND = "checkout";
    private static final String DELETE_BRANCH_COMMAND = "delete_branch";
    private static final String BRANCH_COMMAND = "branch";
    private static final String LOG_COMMAND = "log";
    private static final String MERGE_COMMAND = "merge";
    private static final String STATUS_COMMAND = "status";
    private static final String CLEAN_COMMAND = "clean";
    private static Path rootPath = Paths.get(".");

    /**
     * Разбор непосредственного ввода пользователя к VCS.
     * @param args ввод пользователя.
     */
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
        options.addOption(deleteBranchOption());
        options.addOption(branchOption());
        options.addOption(statusOption());
        options.addOption(cleanOption());

        CommandLineParser parser = new DefaultParser();
        boolean isSomeCommandParsed = false;
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(INIT_COMMAND)) {
                handleInit(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(ADD_COMMAND)) {
                handleAdd(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(REMOVE_COMMAND)) {
                handleRemove(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(RESET_COMMAND)) {
                handleReset(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(COMMIT_COMMAND)) {
                handleCommit(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(NEW_BRANCH_COMMAND)) {
                handleCreateNewBranch(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(CHECKOUT_COMMAND)) {
                handleCheckout(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(LOG_COMMAND)) {
                handleLog(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(MERGE_COMMAND)) {
                handleMerge(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(STATUS_COMMAND)) {
                handleStatus(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(CLEAN_COMMAND)) {
                handleClean(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(DELETE_BRANCH_COMMAND)) {
                handleDeleteBranch(cmd);
                isSomeCommandParsed = true;
            }

            if (cmd.hasOption(BRANCH_COMMAND)) {
                handleBranch(cmd);
                isSomeCommandParsed = true;
            }

            if (!isSomeCommandParsed) {
                printUsage(options);
            }
        } catch (ParseException e) {
            System.out.println("Invalid command");
            printUsage(options);
        }

        if (args.length == 0) {
            printUsage(options);
        }
    }

    /**
     * Вывод справки о пользовании VCS.
     * @param options команды, которые поддерживает VCS.
     */
    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar vcs-1.0-SNAPSHOT.jar", options);
    }

    /**
     * Инициализация команды init
     * @return команда init как опция в Apache Commons CLI
     */
    private static Option initOption() {
        Option initOption = new Option(INIT_COMMAND, true, "Init repository");
        initOption.setArgs(1);
        initOption.setOptionalArg(true);
        return initOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду init
     */
    private static void handleInit(CommandLine cmd) {
        String[] initArgs = cmd.getOptionValues(INIT_COMMAND);
        if (initArgs != null && initArgs.length == 1) {
            rootPath = Paths.get(initArgs[0]).normalize();
        }

        try {
            Repository.initResources(rootPath);
        } catch (IOException | RepositoryAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды add
     * @return команда add как опция в Apache Commons CLI
     */
    private static Option addFileOption() {
        return new Option(ADD_COMMAND, true, "add file(-s)");
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду add
     */
    private static void handleAdd(CommandLine cmd) {
        String[] addArgs = cmd.getOptionValues(ADD_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : addArgs) {
                manager.addFile(Paths.get(file).normalize());
            }
        } catch (IOException | NoRepositoryException | NotRegularFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды rm
     * @return команда rm как опция в Apache Commons CLI
     */
    private static Option rmFileOption() {
        Option deleteOption = new Option(REMOVE_COMMAND, true, "remove file(-s)");
        return deleteOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду rm
     */
    private static void handleRemove(CommandLine cmd) {
        String[] removeArgs = cmd.getOptionValues(ADD_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : removeArgs) {
                manager.removeFile(Paths.get(file).normalize());
            }
        } catch (IOException | NoRepositoryException | NotRegularFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды rm
     * @return команда rm как опция в Apache Commons CLI
     */
    private static Option resetFileOption() {
        Option resetOption = new Option(RESET_COMMAND, true, "reset file(-s)");
        resetOption.setArgs(1);
        return resetOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду rm
     */
    private static void handleReset(CommandLine cmd) {
        String[] resetArgs = cmd.getOptionValues(RESET_COMMAND);
        try {
            VcsManager manager = new VcsManager(rootPath);
            for (String file : resetArgs) {
                manager.reset(Paths.get(file).normalize());
            }
        } catch (IOException | ClassNotFoundException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды commit
     * @return команда commit как опция в Apache Commons CLI
     */
    private static Option commitOption() {
        Option commitOption = new Option(COMMIT_COMMAND, true, "record changes in repository");
        commitOption.setArgs(1);
        return commitOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду commit
     */
    private static void handleCommit(CommandLine cmd) {
        String[] commitArgs = cmd.getOptionValues(COMMIT_COMMAND);
        String message = commitArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.commitChanges(USERNAME, message);
        } catch (IOException | NotRegularFileException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды new_branch
     * @return команда new_branch как опция в Apache Commons CLI
     */
    private static Option newBranchOption() {
        Option branchOption = new Option(NEW_BRANCH_COMMAND, true, "creating new branch");
        branchOption.setArgs(1);
        return branchOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду new_branch
     */
    private static void handleCreateNewBranch(CommandLine cmd) {
        String[] branchArgs = cmd.getOptionValues(NEW_BRANCH_COMMAND);
        String newBranchName = branchArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.checkoutToNewBranch(newBranchName);
        } catch (IOException | BranchAlreadyExistsException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды checkout
     * @return команда checkout как опция в Apache Commons CLI
     */
    private static Option checkoutOption() {
        Option checkoutOption = new Option(CHECKOUT_COMMAND, true, "checkout to existing branch or to hash of commit");
        checkoutOption.setArgs(1);
        return checkoutOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду checkout
     */
    private static void handleCheckout(CommandLine cmd) {
        String[] checkoutArgs = cmd.getOptionValues(CHECKOUT_COMMAND);
        String revision = checkoutArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.checkout(revision);
        } catch (IOException | NoRevisionExistsException | ClassNotFoundException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды delete
     * @return команда delete как опция в Apache Commons CLI
     */
    private static Option deleteBranchOption() {
        Option deleteBranchOption = new Option(DELETE_BRANCH_COMMAND, true, "delete specified branch");
        deleteBranchOption.setArgs(1);
        return deleteBranchOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду delete
     */
    private static void handleDeleteBranch(CommandLine cmd) {
        String[] deleteBranchArgs = cmd.getOptionValues(DELETE_BRANCH_COMMAND);
        String branchName = deleteBranchArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.deleteBranch(branchName);
        } catch (IOException | NoBranchExistsException | NoRepositoryException | CannotDeleteCurrentBranchException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды branch
     * @return команда branch как опция в Apache Commons CLI
     */
    private static Option branchOption() {
        Option branchOption = new Option(BRANCH_COMMAND, true, "get information of current branch or commit");
        branchOption.setArgs(0);
        return branchOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду branch
     */
    private static void handleBranch(CommandLine cmd) {
        try {
            Repository repository = new Repository(rootPath);
            String currentHead = repository.getCurrentHead();
            if (repository.isBranchExists(currentHead)) {
                System.out.println("Current branch name : " + currentHead);
            } else {
                System.out.println("Current hash of commit : " + currentHead);
            }
        } catch (IOException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды log
     * @return команда log как опция в Apache Commons CLI
     */
    private static Option logOption() {
        return new Option(LOG_COMMAND, false, "get current log");
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду log
     */
    private static void handleLog(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.getLog().printLog();
        } catch (IOException | ClassNotFoundException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды merge
     * @return команда merge как опция в Apache Commons CLI
     */
    private static Option mergeOption() {
        Option mergeOption = new Option(MERGE_COMMAND, true, "merge that branch to current");
        mergeOption.setArgs(1);
        return mergeOption;
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду merge
     */
    private static void handleMerge(CommandLine cmd) {
        String[] mergeArgs = cmd.getOptionValues(MERGE_COMMAND);
        String branchName = mergeArgs[0];

        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.merge(branchName);
        } catch (IOException | NotRegularFileException | ClassNotFoundException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды status
     * @return команда status как опция в Apache Commons CLI
     */
    private static Option statusOption() {
        return new Option(STATUS_COMMAND, false, "get description of current state");
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду status
     */
    private static void handleStatus(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.status();
        } catch (IOException | NoRootDirectoryExistsException |
                 ClassNotFoundException | NotRegularFileException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация команды clean
     * @return команда clean как опция в Apache Commons CLI
     */
    private static Option cleanOption() {
        return new Option(CLEAN_COMMAND, false, "remove untracked files from repository");
    }

    /**
     * Вызов соответствующих методов VCS, удолетворяющих запрос пользователя.
     * @param cmd CommandLine от Apache CLI, содержащий команду clean
     */
    private static void handleClean(CommandLine cmd) {
        try {
            VcsManager manager = new VcsManager(rootPath);
            manager.clean();
        } catch (IOException | NoRootDirectoryExistsException |
                 ClassNotFoundException | NotRegularFileException | NoRepositoryException e) {
            e.printStackTrace();
        }
    }
}