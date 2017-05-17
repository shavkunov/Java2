package ru.spbau.shavkunov.myjunit;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainTest {
    @Test
    public void unitTestMain() throws URISyntaxException, IOException {
        Path pathToTestClasses = getPathToTestClasses();

        List<Class<?>> classList = Main.getTestClasses(pathToTestClasses);
        PrintStream printStream = Mockito.mock(PrintStream.class);
        Main.testClasses(pathToTestClasses, printStream);
    }

    private @NotNull Path getPathToTestClasses() {
        return Paths.get(".").resolve("build").resolve("classes").resolve("test");
    }
}