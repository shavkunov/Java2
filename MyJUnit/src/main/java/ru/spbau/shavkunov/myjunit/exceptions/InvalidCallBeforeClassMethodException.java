package ru.spbau.shavkunov.myjunit.exceptions;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.annotations.BeforeClass;

import java.lang.reflect.Method;

/**
 * Exception, which is thrown when call some method annotated as before({@link BeforeClass}) isn't possible.
 */
public class InvalidCallBeforeClassMethodException extends Exception {
    private @NotNull Method method;

    public InvalidCallBeforeClassMethodException(@NotNull Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}
