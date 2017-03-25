package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestConstants {
    public static final Path pathToFile = Paths.get("./src/main/java/ru/spbau/shavkunov/vcs")
                                          .normalize().resolve("VcsObject.java");
    public static final Path rootPath = Paths.get(".").normalize().toAbsolutePath();
}
