package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Test;

public class NotThrowingExceptionAsExceptedTest {
    @Test(expected = Exception.class)
    public void throwFreeTest() {
        int x = 1;
    }
}
