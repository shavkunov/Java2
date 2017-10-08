package ru.spbau.shavkunov.myjunit.primitives;

import org.jetbrains.annotations.NotNull;

/**
 * Result of the test.
 */
public interface TestResult {
    /**
     * Status of the result.
     */
    enum Status {SUCCESSFUL, FAIL, IGNORED}

    /**
     * Returns status of the test.
     * @return status of the test.
     */
    Status getStatus();

    /**
     * Returns result of the test.
     * @return result of the test.
     */
    @NotNull String getResult();
}