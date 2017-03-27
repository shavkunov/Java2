package ru.spbau.shavkunov.vcs;

import ru.spbau.shavkunov.vcs.exceptions.NoRootDirectoryExistsException;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;

import static ru.spbau.shavkunov.vcs.Constants.VCS_FOLDER;

/**
 * Класс, реализующий работу со структурой файлов, не добавленных в репозиторий.
 */
public class FilesTree implements Tree {
    /**
     * Файлы в текущей директории.
     */
    private HashSet<String> files;

    /**
     * Директории в текущей директории.
     */
    private HashSet<FilesTree> subTrees;

    /**
     * Название текущей директории.
     */
    private String prefix;

    /**
     * Создание дерева файлов.
     * @param rootPath корневая директория.
     * @param exceptFiles файлы, которые не должны входить в дерево файлов.
     * @throws NoRootDirectoryExistsException исключение, если не существует корневой директории.
     */
    public FilesTree(Path rootPath, HashSet<String> exceptFiles) throws NoRootDirectoryExistsException {
        files = new HashSet<>();
        subTrees = new HashSet<>();

        if (!rootPath.toFile().exists()) {
            throw new NoRootDirectoryExistsException();
        }

        prefix = ".";
        for (File file : rootPath.toFile().listFiles()) {
            if (file.isDirectory()) {
                subTrees.add(new FilesTree(file.toPath(), file.getName(), exceptFiles));
            } else {
                if (!exceptFiles.contains(file.getPath()) && !file.getPath().contains(VCS_FOLDER)) {
                    files.add(file.getName());
                }
            }
        }
    }

    /**
     * Аналогично основному конструктору, но с инициализацией директории дочерних деревьев.
     * @param rootPath корневая директория.
     * @param exceptFiles файлы, которые не должны входить в дерево файлов.
     * @param prefix название текущей директории.
     * @throws NoRootDirectoryExistsException исключение, если не существует корневой директории.
     */
    private FilesTree(Path rootPath, String prefix, HashSet<String> exceptFiles) throws NoRootDirectoryExistsException {
        this(rootPath, exceptFiles);
        this.prefix = prefix;
    }

    // TODO : ссылка на интерфейс
    @Override
    public void printTree(int spaces) {
        String indent = Tree.multiply("-", spaces + 1);
        String directoryIndent = Tree.multiply("-", spaces);
        System.out.println(directoryIndent + prefix);

        for (String file : files) {
            System.out.println(indent + new File(file).getName());
        }

        for (FilesTree subTree : subTrees) {
            subTree.printTree(spaces + 2);
        }
    }

    // TODO : ссылка на интерфейс
    @Override
    public boolean isFileExists(Path pathToFile) {
        if (files.contains(pathToFile.toString())) {
            return true;
        }

        for (FilesTree subTree : subTrees) {
            if (subTree.isFileExists(pathToFile)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Получение всех файлов, которые находятся в текущей структуре файлов.
     * @return HashSet всех путей файлов.
     */
    public HashSet<String> getAllFiles() {
        HashSet<String> result = new HashSet<>();
        result.addAll(files);

        for (FilesTree tree : subTrees) {
            result.addAll(tree.getAllFiles());
        }

        return result;
    }
}
