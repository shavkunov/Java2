package ru.spbau.shavkunov.vcs;

public class ObjectWithName<T> implements Comparable<ObjectWithName<?>> {
    private T object;
    private String name;

    public ObjectWithName(T object, String name) {
        this.object = object;
        this.name = name;
    }

    public T getContent() {
        return object;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ObjectWithName<?> other) {
        return name.compareTo(other.getName());
    }
}
