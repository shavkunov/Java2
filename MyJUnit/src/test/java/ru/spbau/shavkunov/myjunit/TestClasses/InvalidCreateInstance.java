package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Test;

public class InvalidCreateInstance {
    public InvalidCreateInstance() throws Exception {
        throw new Exception();
    }

    @Test
    public void someTest() {

    }
}
