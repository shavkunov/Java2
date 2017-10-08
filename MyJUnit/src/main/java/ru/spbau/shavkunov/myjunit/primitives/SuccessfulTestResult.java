package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.SUCCESSFUL;

/**
 * Test result that describes the case where test is successfully passed.
 */
public class SuccessfulTestResult implements TestResult {
    /**
     * Method test time.
     */
    private long time;

    /**
     * Testing method.
     */
    private @NotNull Method method;

    /**
     * Initialize basic fields.
     * @param method testing method.
     * @param time testing time.
     */
    public SuccessfulTestResult(@NotNull Method method, long time) {
        this.time = time;
        this.method = method;
    }

    @Override
    public Status getStatus() {
        return SUCCESSFUL;
    }

    @Override
    public @NotNull String getResult() {
        return "Method " + method.getName() + " succesful finished with time " + time + " ms.";
    }
}