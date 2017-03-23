package ru.spbau.shavkunov.vcs;

import org.jetbrains.annotations.NotNull;

/**
 * Обертка над объектов, добавляемая ему имя.
 * @param <T> тип объекта, к которому приписываем имя.
 */
public class ObjectWithName<T> implements Comparable<ObjectWithName<?>> {
    private @NotNull T object;
    private @NotNull String name;

    public ObjectWithName(@NotNull T object, @NotNull String name) {
        this.object = object;
        this.name = name;
    }

    public @NotNull T getContent() {
        return object;
    }

    public @NotNull String getName() {
        return name;
    }

    @Override
    public int compareTo(@NotNull ObjectWithName<?> other) {
        return name.compareTo(other.getName());
    }
}