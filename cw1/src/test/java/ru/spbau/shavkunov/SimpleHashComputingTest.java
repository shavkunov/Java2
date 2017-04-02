package ru.spbau.shavkunov;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertArrayEquals;

public class SimpleHashComputingTest {
    @Before
    public void setup() throws IOException {
        Files.write(Paths.get("test"), "text".getBytes());
    }

    @Test
    public void getHash() throws Exception {
        Path testPath = Paths.get("test");
        SimpleHashComputing hash = new SimpleHashComputing(testPath);

        byte[] array = new byte[] {28, -78, 81, -20, 13, 86, -115, -26, -87, 41, -75, 32, -60, -82, -40, -47};
        assertArrayEquals(array, hash.getHash());
    }

    @Test
    public void tearDown() throws IOException {
        Paths.get("test").toFile().delete();
    }
}