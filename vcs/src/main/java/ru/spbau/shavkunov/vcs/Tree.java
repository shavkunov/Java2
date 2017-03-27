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
    boolean isFileExists(Path pathToFile);

    /**
     * Клонирование строки.
     * @param sample эта строка будет размножена.
     * @param amount число, отвечающие за количество клонов строки sample
     * @return размноженную строку.
     */
    static @NotNull String multiply(@NotNull String sample, int amount) {
        String copy = "";

        for (int i = 0; i < amount; i++) {
            copy += sample;
        }

        return copy;
    }
}
