package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Интерфейс дерева для представления структуры файлов.
 */
interface Tree {
    /**
     * Печать дерева на экран.
     * @param spaces начальный отступ корневой папки.
     */
    void printTree(int spaces);

    /**
     * Проверка существования файла в дереве.
     * @param pathToFile путь к файлу.
     * @return true, если файл присутствует, иначе false.
     */
    boolean isFileExists(@NotNull Path pathToFile);
}
