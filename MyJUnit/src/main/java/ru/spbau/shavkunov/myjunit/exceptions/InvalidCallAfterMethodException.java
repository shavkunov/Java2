package ru.spbau.shavkunov.myjunit.exceptions;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.annotations.After;

import java.lang.reflect.Method;

/**
 * Exception, which is thrown when call some method annotated as before({@link After}) isn't possible.
 */
public class InvalidCallAfterMethodException extends Exception {
    private @NotNull Method method;

    public InvalidCallAfterMethodException(@NotNull Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}