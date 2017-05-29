package ru.spbau.shavkunov.myjunit.exceptions;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.annotations.Before;

import java.lang.reflect.Method;

/**
 * Exception, which is thrown when call some method annotated as before({@link Before}) isn't possible.
 */
public class InvalidCallBeforeMethodException extends Exception {
    private @NotNull Method method;

    public InvalidCallBeforeMethodException(@NotNull Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}