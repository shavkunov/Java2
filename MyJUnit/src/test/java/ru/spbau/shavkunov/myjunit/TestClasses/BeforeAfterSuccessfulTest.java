package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.After;
import ru.spbau.shavkunov.myjunit.annotations.Before;
import ru.spbau.shavkunov.myjunit.annotations.Test;

public class BeforeAfterSuccessfulTest {
    private int status;

    @Before
    public void beforeTest() {
        status = 1;
    }

    @Test
    public void test() {
        status = 2;
    }

    @After
    public void afterTest() {
        status = 3;
    }
}
