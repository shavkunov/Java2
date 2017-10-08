package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.IGNORED;

/**
 * The test result describing the case when the test was annotated
 * as @Test with ignore reason and wasn't tested.
 */
public class IgnoreTestResult implements TestResult {
    /**
     * Method that will be ignored.
     */
    private @NotNull Method method;

    /**
     * Reason why method is ignored.
     */
    private @NotNull String ignoreReason;

    /**
     * Initialize basic fields.
     * @param method test method.
     * @param ignoreReason reason, why method is ignored.
     */
    public IgnoreTestResult(@NotNull Method method, @NotNull String ignoreReason) {
        this.method = method;
        this.ignoreReason = ignoreReason;
    }

    @Override
    public Status getStatus() {
        return IGNORED;
    }

    @Override
    public @NotNull String getResult() {
        return method.getName() + " not tested. Reason: " + ignoreReason;
    }
}