package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.AfterClass;
import ru.spbau.shavkunov.myjunit.annotations.Test;

public class AfterClassFailTest {
    @Test
    public void test() {
    }

    @AfterClass
    public static void afterClass() throws Exception {
        throw new Exception();
    }
}