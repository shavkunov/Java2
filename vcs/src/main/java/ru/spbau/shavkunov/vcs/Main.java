package ru.spbau.shavkunov.vcs;

import ru.spbau.shavkunov.vcs.exceptions.BranchAlreadyExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoBranchExistsException;
import ru.spbau.shavkunov.vcs.exceptions.NoRepositoryException;
import ru.spbau.shavkunov.vcs.exceptions.NotRegularFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static ru.spbau.shavkunov.vcs.Constants.REFERENCE_PREFIX;

// TODO : handle exceptions
public class Main {
    /**
     * Вывод usage пользователю.
     */
    private static void printHelp(String additionalMessage) {
        if (!additionalMessage.equals("")) {
            System.out.println(additionalMessage);
        }
        String message = "Usage: \n" +
                         "init [<Path to repository>] \n" +
                         "add <path to file> \n" +
                         "commit <commit message> \n" +
                         "remove <path to file> \n" +
                         "checkout -b <name of new branch> \n" +
                         "checkout <name of existing branch> \n" +
                         "log";
        System.out.println(message);

    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp("");
            return;
        }

        Path rootPath = Paths.get("").toAbsolutePath();

        switch (args[0]) {
            case "init": {
                if (args.length >= 2) {
                    rootPath = rootPath.resolve(args[1]).normalize();
                }

                try {
                    Repository.initRepository(rootPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "add": {
                if (args.length >= 2) {
                    try {
                        VcsManager manager = new VcsManager(rootPath);
                        for (int i = 1; i < args.length; i++) {
                            manager.addFile(Paths.get(args[i]).normalize());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NotRegularFileException e) {
                        e.printStackTrace();
                    }

                } else {
                    printHelp("No path specified");
                    return;
                }

                break;
            }

            case "remove": {
                if (args.length >= 2) {
                    try {
                        VcsManager manager = new VcsManager(rootPath);
                        for (int i = 1; i < args.length; i++) {
                            manager.removeFile(Paths.get(args[i]).normalize());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NotRegularFileException e) {
                        e.printStackTrace();
                    }

                } else {
                    printHelp("No path specified");
                    return;
                }

                break;
            }

            case "commit": {
                if (args.length < 2) {
                    printHelp("No commit message specified");
                    return;
                }
                String message = args[1];
                if (args.length > 2) {
                    printHelp("Too many arguments");
                    return;
                }

                try {
                    VcsManager manager = new VcsManager(rootPath);
                    manager.commitChanges(System.getProperty(Constants.USER_NAME_PROPERTY), message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NotRegularFileException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }

            case "checkout": {
                if (args.length < 2) {
                    printHelp("Too few arguments");
                    return;
                }

                try {
                    VcsManager manager = new VcsManager(rootPath);
                    if (Objects.equals(args[2], "-b")) {
                        if (args.length < 3) {
                            printHelp("Name of branch not specified");
                            return;
                        }

                        String branchName = args[3];
                        manager.checkoutToNewBranch(branchName);
                    } else {
                        String revision = args[2];
                        manager.checkout(revision);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (BranchAlreadyExistsException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (NoBranchExistsException e) {
                    e.printStackTrace();
                }
            }

            case "branch": {
                if (args.length == 1) {
                    try {
                        Repository repository = Repository.getRepository(rootPath);
                        String currentHead = repository.getCurrentHead();
                        if (currentHead.startsWith(REFERENCE_PREFIX)) {
                            System.out.println("Current branch name : " + currentHead.substring(REFERENCE_PREFIX.length()));
                        } else {
                            System.out.println("Current hash of commit : " + currentHead);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoRepositoryException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (args.length < 4) {
                        printHelp("Too few arguments");
                        return;
                    }

                    try {
                        String branchName = args[4];
                        Repository repository = Repository.getRepository(rootPath);
                        repository.deleteBranch(branchName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoRepositoryException e) {
                        e.printStackTrace();
                    }
                }
            }

            case "log": {

            }

            case "help": {
                printHelp("");
                break;
            }

            default: {
                printHelp("Unknown command");
                break;
            }
        }
    }
}