package ru.spbau.shavkunov.myjunit;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.exceptions.InvalidCallMethodException;
import ru.spbau.shavkunov.myjunit.exceptions.InvalidCreatingInstanceException;
import ru.spbau.shavkunov.myjunit.primitives.TestResult;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI for my JUnit implementation.
 */
public class Main {

    /**
     * Excepted one argument. It's directory with classes. Each extension of test class is .class.
     * These files will be tested.
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Excepted one argument: path to directory");
            return;
        }

        Path directory = Paths.get(args[0]);
        if (directory == null) {
            System.out.println("Invalid path.");
            return;
        }

        try {
            List<Class<?>> classes = getTestClasses(directory);
            for (Class<?> testClass : classes) {
                Tester tester = new Tester(testClass);
                List<TestResult> results = new ArrayList<>();
                try {
                    results = tester.executeClass();
                } catch (InvalidCallMethodException e) {
                    System.out.println("Invocation of method failed in " + testClass);
                    e.printStackTrace();
                } catch (InvalidCreatingInstanceException e) {
                    System.out.println("Creating of" + testClass + "class instance is failed.");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                showResults(results);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show results of testing class file.
     * @param results testing method results of class.
     */
    private static void showResults(@NotNull List<TestResult> results) {
        int ignoredTests = 0;
        int successfulTests = 0;
        int failTests = 0;
        for (TestResult testResult : results) {
            System.out.println(testResult.getResult());

            switch (testResult.getStatus()) {
                case IGNORED:
                    ignoredTests++;

                case SUCCESSFUL:
                    successfulTests++;

                case FAIL:
                    failTests++;
            }
        }

        int total = ignoredTests + successfulTests + failTests;
        String finalMessage;
        if (failTests == 0) {
            finalMessage = "Testing was completed. All " + total + " tests passed.";
        } else {
            finalMessage = "Testing was completed. Total tests: " + total +
                           "\n Succeeded: " + successfulTests +
                           "\n Ignored tests: " + ignoredTests +
                           "\n Failed tests: " + failTests + ".";
        }

        System.out.println(finalMessage);
    }

    /**
     * Gets list of all classes in specified directory.
     * @param directory directory with
     * @return list of loaded class files.
     * @throws IOException if any IO exception occurs during load test classes.
     */
    private static @NotNull List<Class<?>> getTestClasses(@NotNull Path directory) throws IOException {
        ArrayList<Path> fileClasses = new ArrayList<>();
        Files.walk(directory).filter(Files::isRegularFile)
                             .filter(file -> file.endsWith(".class"))
                             .forEach(file -> fileClasses.add(file));
        ClassLoader classLoader = new URLClassLoader(new URL[]{directory.toUri().toURL()});

        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Path path : fileClasses) {
            Class loadedClass = null;

            String pathToClass = path.toFile().getName();
            try {
                loadedClass = classLoader.loadClass(pathToClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (loadedClass != null) {
                classes.add(loadedClass);
            }
        }

        return classes;
    }
}