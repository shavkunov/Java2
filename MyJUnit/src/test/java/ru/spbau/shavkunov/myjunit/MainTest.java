package ru.spbau.shavkunov.myjunit;

import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainTest {
    @Test
    public void testMain() throws URISyntaxException {
        Path pathToTestClasses = Paths.get(getClass().getResource(".").toURI()).resolve("TestClasses");
        String[] classesDirectory = new String[] {pathToTestClasses.toString()};
        Main.main(classesDirectory);
    }
}
