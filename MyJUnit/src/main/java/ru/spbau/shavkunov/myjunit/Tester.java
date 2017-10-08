package ru.spbau.shavkunov.myjunit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.shavkunov.myjunit.annotations.*;
import ru.spbau.shavkunov.myjunit.exceptions.*;
import ru.spbau.shavkunov.myjunit.primitives.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static ru.spbau.shavkunov.myjunit.annotations.Test.EMPTY_REASON;

/**
 * Instance of Tester class is able to test specified class.
 */
public class Tester {
    /**
     * Methods of test class annotated as {@link BeforeClass}
     */
    private @NotNull ArrayList<Method> methodsBeforeClass;

    /**
     * Methods of test class annotated as {@link AfterClass}
     */
    private @NotNull ArrayList<Method> methodsAfterClass;

    /**
     * Methods of test class annotated as {@link Before}
     */
    private @NotNull ArrayList<Method> methodsBefore;

    /**
     * Methods of test class annotated as {@link After}
     */
    private @NotNull ArrayList<Method> methodsAfter;

    /**
     * Methods of test class annotated as {@link Test}
     */
    private @NotNull ArrayList<Method> testMethods;

    /**
     * Class that will be tested.
     */
    private @NotNull Class testClass;

    /**
     * Creates the tester.
     * @param testClass class that need to test.
     */
    public Tester(@NotNull Class testClass) {
        this.testClass = testClass;
        methodsBeforeClass = new ArrayList<>();
        methodsAfterClass = new ArrayList<>();
        methodsBefore = new ArrayList<>();
        methodsAfter = new ArrayList<>();
        testMethods = new ArrayList<>();
    }

    /**
     * Execute all methods annotated with
     * {@link Test}, {@link Before}, {@link After}, {@link BeforeClass}, {@link AfterClass}.
     * @return returns test results of methods annotated as {@link Test}.
     * @throws InvalidCreatingInstanceException occurs, if creating instance of test class is failed.
     * @throws InvalidCallMethodException occurs, if method.invoke is failed.
     * @throws IllegalAccessException if access violation is occurs.
     */
    public List<TestResult> executeClass()
            throws InvalidCreatingInstanceException, InvalidCallMethodException, IllegalAccessException, InvalidCallBeforeMethodException, InvalidCallAfterMethodException, InvalidCallAfterClassMethodException, InvalidCallBeforeClassMethodException {
        ArrayList<TestResult> testResults = new ArrayList<>();
        divideMethodsIntoGroups(testClass);

        try {
            callMethods(methodsBeforeClass, null);
        } catch (InvalidCallMethodException exception) {
            throw new InvalidCallBeforeClassMethodException(exception.getMethod());
        }

        for (Method method : testMethods) {
            Annotation annotation = method.getAnnotation(Test.class);
            Test test = (Test) annotation;

            Class<? extends Exception> expectedException = test.expected();
            String ignoreReason = test.ignoreReason();

            if (!ignoreReason.equals(EMPTY_REASON)) {
                testResults.add(new IgnoreTestResult(method, ignoreReason));
                continue;
            }

            Object instance = null;
            try {
                instance = testClass.newInstance();
            } catch (Exception e) {
                throw new InvalidCreatingInstanceException();
            }

            testResults.add(testMethod(method, instance, expectedException));
        }

        try {
            callMethods(methodsAfterClass, null);
        } catch (InvalidCallMethodException exception) {
            throw new InvalidCallAfterClassMethodException(exception.getMethod());
        }

        return testResults;
    }

    /**
     * Tests specified method.
     * @param method method to test.
     * @param instance underlying instance of class, where method is located.
     * @param expectedException exception, that method can produce.
     * @return test result of the specified method.
     * @throws InvalidCallMethodException occurs, if method.invoke is failed.
     * @throws IllegalAccessException if access violation is occurs.
     */
    private TestResult testMethod(@NotNull Method method, @NotNull Object instance,
                                  @NotNull Class<? extends Exception> expectedException)
            throws InvalidCallMethodException, IllegalAccessException, InvalidCallBeforeMethodException, InvalidCallAfterMethodException {
        long startTime = System.currentTimeMillis();
        long elapsedTime;
        try {
            try {
                callMethods(methodsBefore, instance);
            } catch (InvalidCallMethodException exception) {
                throw new InvalidCallBeforeMethodException(exception.getMethod());
            }

            method.invoke(instance);

            try {
                callMethods(methodsAfter, instance);
            } catch (InvalidCallMethodException exception) {
                throw new InvalidCallAfterMethodException(exception.getMethod());
            }

        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (expectedException.isInstance(target)) {
                // we've got expected exception
                elapsedTime = System.currentTimeMillis() - startTime;
                ExceptedTestResult exceptedTestResult = new ExceptedTestResult(method, exception, elapsedTime);

                return exceptedTestResult;
            }

            // we've got exception, that hasn't to be expected.
            elapsedTime = System.currentTimeMillis() - startTime;
            UnexpectedTestResult unexpectedTestResult = new UnexpectedTestResult(method, expectedException,
                                                                                         exception, elapsedTime);

            return unexpectedTestResult;
        }

        if (!expectedException.equals(TestException.class)) {
            elapsedTime = System.currentTimeMillis() - startTime;
            NoExceptedExceptionTestResult failTest = new NoExceptedExceptionTestResult(method, expectedException,
                                                                                       elapsedTime);

            return failTest;
        }

        elapsedTime = System.currentTimeMillis() - startTime;;
        SuccessfulTestResult successfulTestResult = new SuccessfulTestResult(method, elapsedTime);
        return successfulTestResult;
    }

    /**
     * Call specified methods of the class.
     * @param methods methods, that need to be invoked.
     * @param instance instance of class, where methods are located.
     * @throws InvalidCallMethodException occurs, if method.invoke is failed.
     */
    private void callMethods(@NotNull ArrayList<Method> methods, @Nullable Object instance) throws InvalidCallMethodException {
        for (Method method : methods) {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new InvalidCallMethodException(method);
            }
        }
    }

    /**
     * Dividing all methods of specified test class into 5 method groups:
     * 1) methods annotated as {@link Before}
     * 2) methods annotated as {@link After}
     * 3) methods annotated as {@link BeforeClass}
     * 4) methods annotated as {@link AfterClass}
     * 5) methods annotated as {@link Test}
     * @param testClass specified class.
     */
    private void divideMethodsIntoGroups(@NotNull Class testClass) {
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(BeforeClass.class)) {
                methodsBeforeClass.add(method);
            }

            if (method.isAnnotationPresent(AfterClass.class)) {
                methodsAfterClass.add(method);
            }

            if (method.isAnnotationPresent(After.class)) {
                methodsAfter.add(method);
            }

            if (method.isAnnotationPresent(Before.class)) {
                methodsBefore.add(method);
            }

            if (method.isAnnotationPresent(Test.class)) {
                testMethods.add(method);
            }
        }
    }
}