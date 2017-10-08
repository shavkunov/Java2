package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.After;
import ru.spbau.shavkunov.myjunit.annotations.Test;

public class AfterFailTest {
    @Test
    public void someTest() {

    }

    @After
    public void failAfter() {
        throw new RuntimeException();
    }
}
