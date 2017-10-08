package ru.spbau.shavkunov.myjunit.exceptions;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.annotations.AfterClass;

import java.lang.reflect.Method;

/**
 * Exception, which is thrown when call some method annotated as before({@link AfterClass}) isn't possible.
 */
public class InvalidCallAfterClassMethodException extends Exception {
    private @NotNull Method method;

    public InvalidCallAfterClassMethodException(@NotNull Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}
