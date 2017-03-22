package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;

/**
 * Интерфейс для объекта в системе контроля версий. Каждый файл, может узнать путь к самому себе.
 */
interface VcsObject {
    /**
     * Получение пути объекта к самому себе.
     * @param repository репозиторий, в котором находится объект.
     * @return путь объекта к самому себе.
     */
    Path getPathToObject(Repository repository);
}