package ru.spbau.shavkunov.vcs;

/**
 * Набор констант, необходимых для работы с репозиторием.
 */
public class Constants {
    public static final String OBJECTS_FOLDER = "objects";
    public static final String REFERENCES_FOLDER = "references";
    public static final String VCS_FOLDER = ".vcs";
    public static final String HEAD = "head";
    public static final String DEFAULT_BRANCH_NAME = "master";
    public static final String INDEX_FILE = "index";
    public static final String USER_NAME_PROPERTY = "user.name";
    public static final String USERNAME = System.getProperty(Constants.USER_NAME_PROPERTY);
    public static final String REFERENCE_PREFIX = "ref: ";
    public static final String MERGE_MESSAGE = "Merged : ";
    public static final String UNTRACKED_MESSAGE = "Untracked files: ";
    public static final String MODIFIED_MESSAGE = "Modified files: ";
    public static final String STAGED_MESSAGE = "Staged files: ";
    public static final String DELETED_MESSAGE = "Deleted files: ";
}