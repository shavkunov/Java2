package ru.spbau.shavkunov.vcs.primitives;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.vcs.VcsManager;

import java.io.IOException;

/**
 * Класс, отвечающий за представление объекта reference(ссылка) в системе контроля версий.
 * Ссылка указывает на текущую ветку и хеш коммита, отвечающий за текущее состояние репозитория.
 */
public class Reference {
    private static final Logger logger = LoggerFactory.getLogger(VcsManager.class);

    /**
     * Название файла в папке refs
     */
    private @NotNull String name;

    /**
     * Содержимое файла в папке refs.
     */
    private @NotNull String commitHash;

    /**
     * Создание объекта ссылки репозитория. Если в head лежит название ветки, то это будет стандартной ссылкой, иначе
     * ссылке будет хранится только хеш текущего коммита.
     * @param name имя ссылки
     * @param commitHash имя коммита, на который указывает ссылка.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public Reference(@NotNull String name, @NotNull String commitHash) throws IOException {
        this.name = name;
        this.commitHash = commitHash;

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
        commitHash = repository.getReferenceCommitHash(name);
    }

    /**
     * Получение хеша коммита ссылки.
     * @return хеш коммита.
     */
    public @NotNull String getCommitHash() {
        return commitHash;
    }

    /**
     * Получение имени ссылки.
     * @return имя ссылки.
     */
    public String getName() {
        return name;
    }

    /**
     * Обновление информации о текущем коммите.
     * @param newCommitHash новый хеш коммита.
     * @param repository репозиторий, где требуется обновить информацию.
     * @throws IOException исключение, если возникли проблемы с чтением файла.
     */
    public void refreshCommitHash(@NotNull String newCommitHash, @NotNull Repository repository) throws IOException {
        repository.storeReferenceCommit(name, newCommitHash);
        commitHash = newCommitHash;
    }
}