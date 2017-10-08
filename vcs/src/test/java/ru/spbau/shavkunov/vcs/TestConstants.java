package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestConstants {
    public static final Path rootPath = Paths.get(".").normalize();
    public static final Path pathToFile = rootPath.resolve("test1");
}
