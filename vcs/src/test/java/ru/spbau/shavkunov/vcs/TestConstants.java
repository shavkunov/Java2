package ru.spbau.shavkunov.vcs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestConstants {
    public static final Path pathToVcs = Paths.get("./src/main/java/ru/spbau/shavkunov/vcs").normalize();
    public static final Path rootPath = Paths.get(".").normalize().toAbsolutePath();
}
