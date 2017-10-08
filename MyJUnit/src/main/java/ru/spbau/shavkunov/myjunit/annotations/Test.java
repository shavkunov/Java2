package ru.spbau.shavkunov.myjunit.annotations;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.exceptions.TestException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated as {@link Test} will be recognized as test-case methods.
 * Test can be ignored if the {@code ignore} reason is specified.
 * Test is successful if it doesn't throw any exception and the parameter {@code expected} is unassigned, or it throws
 * an exception which is instance of the exception type contained in {@code expected} parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    /**
     * Represents empty reason, if test isn't ignored.
     */
    String EMPTY_REASON = "";

    /**
     * Excepted exception that is thrown during the test.
     * @return exception type.
     */
    @NotNull Class<? extends Exception> expected() default TestException.class;

    /**
     * Ignore the test.
     * @return the reason of ignoring.
     */
    @NotNull String ignoreReason() default EMPTY_REASON;
}