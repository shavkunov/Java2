package ru.spbau.shavkunov.myjunit.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Exception, which is thrown when call method isn't possible.
 */
public class InvalidCallMethodException extends Exception {
    private @Nullable Method method;

    public InvalidCallMethodException(@NotNull Method method) {
        this.method = method;
    }

    public InvalidCallMethodException() {
    }

    public Method getMethod() {
        return method;
    }
}