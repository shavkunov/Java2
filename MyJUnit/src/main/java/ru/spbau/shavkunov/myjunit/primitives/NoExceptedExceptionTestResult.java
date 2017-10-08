package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.FAIL;

/**
 * The test result describing the case when the test method didn't throw any exception, but it was expected.
 */
public class NoExceptedExceptionTestResult implements TestResult {
    /**
     * Method test time.
     */
    private long time;

    /**
     * This exception is excepted.
     */
    private @NotNull Class<? extends Exception> expectedException;

    /**
     * Testing method.
     */
    private @NotNull Method method;

    /**
     * Initialize basic fields.
     * @param method testing method.
     * @param expectedException excepted exception.
     * @param time testing time.
     */
    public NoExceptedExceptionTestResult(@NotNull Method method,
                                         @NotNull Class<? extends Exception> expectedException, long time) {
        this.method = method;
        this.expectedException = expectedException;
        this.time = time;
    }

    @Override
    public Status getStatus() {
        return FAIL;
    }

    @Override
    public @NotNull String getResult() {
        return "Method " + method.getName() + " finished with time " + time + " ms.\n" +
               "Excepted : " + expectedException;
    }
}