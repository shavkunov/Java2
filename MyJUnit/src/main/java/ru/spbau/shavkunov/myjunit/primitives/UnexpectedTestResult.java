package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.FAIL;

/**
 * The test result describing the case when the test method threw an exception,
 * but no exception was expected or we expected different one.
 */
public class UnexpectedTestResult implements TestResult {
    /**
     * Testing time of the method.
     */
    private long time;

    /**
     * Expected, that this exception will be produced by method.
     */
    private @NotNull Class<? extends Exception> expectedException;

    /**
     * Caught exception produced by method. This isn't equals to exceptedException.
     */
    private @NotNull Exception caughtException;

    /**
     * Testing method.
     */
    private @NotNull Method method;

    /**
     * Initialize all basic fields.
     * @param method testing method.
     * @param expectedException excepted exception.
     * @param caughtException caught exception.
     * @param time testing time.
     */
    public UnexpectedTestResult(@NotNull Method method,
                                @NotNull Class<? extends Exception> expectedException,
                                @NotNull Exception caughtException,
                                long time) {
        this.expectedException = expectedException;
        this.caughtException = caughtException;
        this.method = method;
        this.time = time;
    }

    @Override
    public Status getStatus() {
        return FAIL;
    }

    @Override
    public @NotNull String getResult() {
        return "Method " + method.getName() + " finished with time " + time + " ms.\n" +
               "Excepted:" + expectedException.getClass() + " but got: " + caughtException.getClass();
    }
}