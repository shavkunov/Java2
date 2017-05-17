package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Test;

public class SuccessTest {
    public static final String reason = "ignore reason";

    @Test(ignoreReason = reason)
    public void ignoredTest() throws Exception {
        throw new Exception();
    }

    @Test
    public void successfulTest() {

    }
}
