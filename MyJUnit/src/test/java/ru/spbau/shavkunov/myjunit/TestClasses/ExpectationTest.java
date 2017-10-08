package ru.spbau.shavkunov.myjunit.TestClasses;

import ru.spbau.shavkunov.myjunit.annotations.Test;

import java.util.NoSuchElementException;

public class ExpectationTest {
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void throwFreeMethod() {
    }

    @Test(expected = NoSuchElementException.class)
    public void throwNoSuchElementException() {
        throw new NoSuchElementException();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void throwUnexpectedNoSuchElementException() {
        throw new NoSuchElementException();
    }
}