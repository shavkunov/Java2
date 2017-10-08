package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Test;

public class FailTest {
    @Test
    public void failMethod() {
        throw new RuntimeException();
    }
}
