package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.spbau.shavkunov.vcs.Constants.*;

/**
 * Класс, отвечающий за представление объекта reference(ссылка) в системе контроля версий.
 * Ссылка указывает на текущую ветку и хеш коммита, отвечающий за текущее состояние репозитория.
 */
public class Reference implements VcsObject {
    private static final Logger logger = LoggerFactory.getLogger(VcsManager.class);

    /**
     * Название файла в папке refs
     */
    private @NotNull String name;

    /**
     * Содержимое файла в папке refs.
     */
    private @NotNull String commitHash;

    @Override
    public @NotNull Path getPathToObject(@NotNull Repository repository) {
        return repository.getReferencesPath().resolve(name);
    }

    /**
     * Создание объекта ссылки репозитория. Если в head лежит название ветки, то это будет стандартной ссылкой, иначе
     * ссылке будет хранится только хеш текущего коммита.
     * @param repository репозиторий, где создается ссылка.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Reference(@NotNull Repository repository) throws IOException {
        String head = repository.getCurrentHead();
        if (head.startsWith(REFERENCE_PREFIX)) {
            name = head.substring(REFERENCE_PREFIX.length());
            commitHash = Repository.getFirstLine(repository.getReferencesPath().resolve(name));
        } else {
            name = "Commit hash";
            commitHash = head;
        }

        logger.debug("Created reference. Commit hash : " + commitHash);
    }

    /**
     * Создание ссылки конкретной ветки.
     * @param name имя ветки.
     * @param repository репозиторий, где создается ссылка.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Reference(@NotNull String name, @NotNull Repository repository) throws IOException {
        this.name = name;
        commitHash = Repository.getFirstLine(repository.getReferencesPath().resolve(name));
    }

    public @NotNull String getCommitHash() {
        return commitHash;
    }

    /**
     * Обновление информации о текущем коммите.
     * @param newCommitHash новый хеш коммита.
     * @param repository репозиторий, где требуется обновить информацию.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void refreshCommitHash(@NotNull String newCommitHash, @NotNull Repository repository) throws IOException {
        Files.write(getPathToObject(repository), newCommitHash.getBytes());
    }
}