package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.BeforeClass;
import ru.spbau.shavkunov.myjunit.annotations.Test;

import java.io.IOException;

public class BeforeClassFailTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        throw new IOException();
    }

    @Test
    public void test() {
    }
}
