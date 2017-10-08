package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.FAIL;

/**
 * Test result, where method produced excepted exception.
 */
public class ExceptedTestResult implements TestResult {
    /**
     * Test excepting this exception produced by method.
     */
    private @NotNull Exception exceptedException;

    /**
     * Method test time.
     */
    private long time;

    /**
     * Testing method.
     */
    private @NotNull Method method;

    /**
     * Initialize all basic fields.
     * @param method test method.
     * @param exceptedException excepted this exception produced by method.
     * @param time method test time.
     */
    public ExceptedTestResult(@NotNull Method method, @NotNull Exception exceptedException, long time) {
        this.exceptedException = exceptedException;
        this.method = method;
        this.time = time;
    }

    @Override
    public Status getStatus() {
        return FAIL;
    }

    @Override
    public @NotNull String getResult() {
        return "Method " + method.getName() + " finished with time:" + time +
               " ms. Excepted exception : " + exceptedException.getClass();
    }
}