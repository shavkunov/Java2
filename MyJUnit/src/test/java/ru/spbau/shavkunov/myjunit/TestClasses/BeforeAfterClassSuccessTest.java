package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.AfterClass;
import ru.spbau.shavkunov.myjunit.annotations.BeforeClass;
import ru.spbau.shavkunov.myjunit.annotations.Test;

public class BeforeAfterClassSuccessTest {
    private static int status;

    @BeforeClass
    public static void beforeClass() {
        status = 0;
    }

    @Test
    public void test() {
        status = 1;
    }

    @AfterClass
    public static void afterClass() {
        status = 2;
    }
}
