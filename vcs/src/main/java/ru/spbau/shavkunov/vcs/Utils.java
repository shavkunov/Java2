package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Вспомогательный класс с методами, которые могут быть использоваными другими классами.
 */
public class Utils {
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

    /**
     * Получение первой строчки файла.
     * @param pathToFile путь к файлу.
     * @return первая строчка данного файла.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     */
    public static @NotNull String getFirstLine(@NotNull Path pathToFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));
        String line = reader.readLine();
        if (line == null) {
            line = "";
        }

        return line;
    }
}
