package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Before;
import ru.spbau.shavkunov.myjunit.annotations.Test;

public class BeforeFailTest {
    @Before
    public void failBefore() {
        throw new RuntimeException();
    }

    @Test
    public void someTest() {

    }
}
