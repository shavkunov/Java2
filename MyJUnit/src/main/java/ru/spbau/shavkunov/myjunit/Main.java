package ru.spbau.shavkunov.myjunit;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.myjunit.exceptions.*;
import ru.spbau.shavkunov.myjunit.primitives.TestResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
            System.out.println("Expected one argument: path to directory");
            return;
        }

        Path directory = Paths.get(args[0]);
        if (directory == null) {
            System.out.println("Invalid path.");
            return;
        }

        testClasses(directory, System.out);
    }

    /**
     * Tests class files located in specified directory in write results in specified stream.
     * @param directory directory, where class files are located.
     * @param printStream print stream, where result of tests will be written.
     */
    public static void testClasses(@NotNull Path directory, @NotNull PrintStream printStream) {
        try {
            List<Class<?>> classes = getTestClasses(directory);
            for (Class<?> testClass : classes) {
                printStream.println("Testing class:" + testClass);
                Tester tester = new Tester(testClass);
                List<TestResult> results = new ArrayList<>();
                try {
                    results = tester.executeClass();
                } catch (InvalidCallMethodException e) {
                    printStream.println("Invocation of method" + e.getMethod() + " failed in " + testClass);
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (InvalidCreatingInstanceException e) {
                    printStream.println("Creating of" + testClass + "class instance is failed.");
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (IllegalAccessException e) {
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (InvalidCallBeforeMethodException e) {
                    printStream.println("Invocation of before method" + e.getMethod() + " failed in " + testClass);
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (InvalidCallAfterMethodException e) {
                    printStream.println("Invocation of after method" + e.getMethod() + " failed in " + testClass);
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (InvalidCallBeforeClassMethodException e) {
                    printStream.println("Invocation of before class method" + e.getMethod() + " failed in " + testClass);
                    printStream.println(Arrays.toString(e.getStackTrace()));
                } catch (InvalidCallAfterClassMethodException e) {
                    printStream.println("Invocation of after class method" + e.getMethod() + " failed in " + testClass);
                    printStream.println(Arrays.toString(e.getStackTrace()));
                }

                if (results.size() == 0) {
                    printStream.println("Class " + testClass + " has no tests");
                }

                showResults(results, testClass, printStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show results of testing class file.
     * @param results testing method results of class.
     * @param testClass underlying test class.
     * @param printStream stream, where result will be written.
     */
    private static void showResults(@NotNull List<TestResult> results,
                                    @NotNull Class testClass,
                                    @NotNull PrintStream printStream) {
        int ignoredTests = 0;
        int successfulTests = 0;
        int failTests = 0;
        for (TestResult testResult : results) {
            printStream.println(testResult.getResult());

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
            finalMessage = "Testing of " + testClass + " was completed. All " + total + " tests passed";
        } else {
            finalMessage = "Testing of " + testClass + " was completed. " +
                           "\nTotal tests: " + total +
                           "\nSucceeded: " + successfulTests +
                           "\nIgnored tests: " + ignoredTests +
                           "\nFailed tests: " + failTests;
        }

        printStream.println(finalMessage + "\n");
    }

    /**
     * Gets list of all classes in specified directory.
     * @param directory directory with
     * @return list of loaded class files.
     * @throws IOException if any IO exception occurs during load test classes.
     */
    public static @NotNull List<Class<?>> getTestClasses(@NotNull Path directory) throws IOException {
        List<Path> fileClasses = Files.walk(directory)
                                       .map(directory::relativize)
                                       .filter(path -> path.toString().endsWith(".class"))
                                       .collect(Collectors.toList());

        ClassLoader classLoader = new URLClassLoader(new URL[]{directory.toUri().toURL()});

        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Path path : fileClasses) {
            Class loadedClass = null;
            String pathToClassWithExtension = path.toString().replace(File.separatorChar, '.');
            String pathToClass = pathToClassWithExtension.substring(0, pathToClassWithExtension.lastIndexOf('.'));

            try {
                loadedClass = classLoader.loadClass(pathToClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (loadedClass != null) {
                classes.add(loadedClass);
            } else {
                System.out.println("Class " + pathToClass + " not found");
            }
        }

        return classes;
    }
}