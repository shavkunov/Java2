package ru.spbau.shavkunov;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        Path pathToFile = Paths.get(args[0]);
        SimpleHashComputing simpleHash = new SimpleHashComputing(pathToFile);
        ConcurrentHashComputing concurrentHash = new ConcurrentHashComputing(pathToFile);

        try {
            long startTimeSimpleHashComputing = System.currentTimeMillis();
            byte[] simpleHashRes = simpleHash.getHash();
            long estimatedTimeSimpleHashComputing = System.currentTimeMillis() - startTimeSimpleHashComputing;

            System.out.println("MD5 hash : ");
            for (byte b :simpleHashRes) {
                System.out.print(b);
            }
            System.out.println("\nTime elapsed with one thread : " + estimatedTimeSimpleHashComputing + " ms");

            long startTimeConcurrentHashComputing = System.currentTimeMillis();
            byte[] concurrentHashRes = concurrentHash.getHash();
            long estimatedTimeConcurrentHashComputing = System.currentTimeMillis() - startTimeConcurrentHashComputing;

            System.out.println("MD5 hash : ");
            for (byte b : concurrentHashRes) {
                System.out.print(b);
            }
            System.out.println("\nTime elapsed with multi threads : " + estimatedTimeConcurrentHashComputing + " ms");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
