package ru.spbau.shavkunov.vcs.utils;

/**
 * Набор констант, необходимых для работы с репозиторием.
 */
public class Constants {
    /**
     * Папка объектов репозитория.
     */
    public static final String OBJECTS_FOLDER = "objects";

    /**
     * Папка ссылок репозитория.
     */
    public static final String REFERENCES_FOLDER = "references";

    /**
     * Папка репозитория.
     */
    public static final String VCS_FOLDER = ".vcs";

    /**
     * Head файл репозитория.
     */
    public static final String HEAD = "head";

    /**
     * Стандартная ветка репозтория -- master.
     */
    public static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * Название index файла репозитория.
     */
    public static final String INDEX_FILE = "index";

    /**
     * Имя пользователя.
     */
    public static final String USERNAME = System.getProperty("user.name");

    /**
     * Префикс для обозначения ссылки.
     */
    public static final String REFERENCE_PREFIX = "ref: ";

    /**
     * Сообщение для merge коммита.
     */
    public static final String MERGE_MESSAGE = "Merged : ";

    /**
     * Сообщение перед выводом недобавленных файлов в репозиторий.
     */
    public static final String UNTRACKED_MESSAGE = "Untracked files: ";

    /**
     * Сообщение перед выводом измененных файлов.
     */
    public static final String MODIFIED_MESSAGE = "Modified files: ";

    /**
     * Сообщение перед выводом добавленных файлов.
     */
    public static final String STAGED_MESSAGE = "Staged files: ";

    /**
     * Сообщение перед выводом удаленных файлов.
     */
    public static final String DELETED_MESSAGE = "Deleted files: ";
}