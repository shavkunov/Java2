package ru.spbau.shavkunov.vcs.primitives;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Обертка над объектом, которая приписывает ему имя.
 * @param <T> тип объекта, к которому приписываем имя.
 */
public class ObjectWithName<T> implements Comparable<ObjectWithName<?>>, Serializable {
    /**
     * Сам объект, который хранится внутри.
     */
    private @NotNull T object;

    /**
     * И его имя.
     */
    private @NotNull String name;

    public ObjectWithName(@NotNull T object, @NotNull String name) {
        this.object = object;
        this.name = name;
    }

    /**
     * Получение внутреннего объекта.
     * @return объект.
     */
    public @NotNull T getContent() {
        return object;
    }

    /**
     * Получение имени объекта
     * @return имя объекта.
     */
    public @NotNull String getName() {
        return name;
    }

    @Override
    public int compareTo(@NotNull ObjectWithName<?> other) {
        return name.compareTo(other.getName());
    }
}