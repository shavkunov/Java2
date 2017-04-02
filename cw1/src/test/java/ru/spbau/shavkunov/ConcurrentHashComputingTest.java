package ru.spbau.shavkunov;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertArrayEquals;

public class ConcurrentHashComputingTest {
    @Test
    public void getHash() throws Exception {
        Path pathToFile = Paths.get("src");
        SimpleHashComputing simpleHash = new SimpleHashComputing(pathToFile);
        ConcurrentHashComputing concurrentHash = new ConcurrentHashComputing(pathToFile);

        byte[] simpleHashRes = simpleHash.getHash();
        byte[] concurrentHashRes = concurrentHash.getHash();

        assertArrayEquals(simpleHashRes, concurrentHashRes);
    }
}