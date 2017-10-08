package ru.spbau.shavkunov.myjunit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import ru.spbau.shavkunov.myjunit.exceptions.*;
import ru.spbau.shavkunov.myjunit.primitives.*;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.FAIL;
import static ru.spbau.shavkunov.myjunit.primitives.TestResult.Status.SUCCESSFUL;

public class JunitTest {
    private static String classPrefix = "ru.spbau.shavkunov.myjunit.TestClasses";
    private static Matcher successfulMatcher = new SuccessfulResultMatcher();
    private static Matcher ignoreMatcher = new IgnoreTestResultMatcher();
    private static Matcher expectedMatcher = new ExceptedTestResultMatcher();
    private static Matcher unexpectedMatcher = new UnexpectedTestResultMatcher();
    private static Matcher noexpectedMatcher = new NoExceptedExceptionMatcher();

    @Test
    public void successfulTest() throws Exception {

        Class testClass = Class.forName(classPrefix + ".SuccessTest");
        Tester tester = new Tester(testClass);
        List<TestResult> result = tester.executeClass();

        assertThat(result, containsInAnyOrder(successfulMatcher, ignoreMatcher));
    }

    @Test
    public void emptyClassTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".EmptyTest");
        Tester tester = new Tester(testClass);
        List<TestResult> result = tester.executeClass();

        assertThat(result, is(empty()));
    }

    @Test
    public void testExceptedAndUnexpectedExceptions() throws Exception {
        Class testClass = Class.forName(classPrefix + ".ExpectationTest");
        Tester tester = new Tester(testClass);
        List<TestResult> result = tester.executeClass();

        assertThat(result, containsInAnyOrder(expectedMatcher, noexpectedMatcher, unexpectedMatcher));
    }

    @Test
    public void failTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".FailTest");
        Tester tester = new Tester(testClass);
        List<TestResult> result = tester.executeClass();

        assertThat(result.get(0) instanceof UnexpectedTestResult, is(true));
        assertThat(result.get(0).getStatus(), is(FAIL));
    }

    @Test
    public void successfulBeforeAfterTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".BeforeAfterSuccessfulTest");
        Tester tester = new Tester(testClass);
        List<TestResult> result = tester.executeClass();

        assertThat(result.get(0) instanceof SuccessfulTestResult, is(true));
        assertThat(result.get(0).getStatus(), is(SUCCESSFUL));
    }

    @Test(expected = InvalidCallMethodException.class)
    public void throwNull() throws InvalidCallMethodException {
        throw new InvalidCallMethodException();
    }

    @Test(expected = InvalidCallBeforeMethodException.class)
    public void beforeFailTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".BeforeFailTest");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    @Test(expected = InvalidCallAfterMethodException.class)
    public void afterFailTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".AfterFailTest");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    @Test
    public void successfulBeforeAfterClassTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".BeforeAfterClassSuccessTest");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    @Test(expected = InvalidCallBeforeClassMethodException.class)
    public void beforeClassFailTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".BeforeClassFailTest");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    @Test(expected = InvalidCallAfterClassMethodException.class)
    public void afterClassFailTest() throws Exception {
        Class testClass = Class.forName(classPrefix + ".AfterClassFailTest");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    @Test(expected = InvalidCreatingInstanceException.class)
    public void invalidCreateInstance() throws Exception {
        Class testClass = Class.forName(classPrefix + ".InvalidCreateInstance");
        Tester tester = new Tester(testClass);
        tester.executeClass();
    }

    private static class SuccessfulResultMatcher extends BaseMatcher<TestResult> {
        @Override
        public boolean matches(Object item) {
            return item instanceof SuccessfulTestResult;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(SuccessfulTestResult.class);
        }
    }

    private static class ExceptedTestResultMatcher extends BaseMatcher<TestResult> {
        @Override
        public boolean matches(Object item) {
            return item instanceof ExceptedTestResult;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(ExceptedTestResult.class);
        }
    }

    private static class UnexpectedTestResultMatcher extends BaseMatcher<TestResult> {
        @Override
        public boolean matches(Object item) {
            return item instanceof UnexpectedTestResult;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(UnexpectedTestResult.class);
        }
    }

    private static class IgnoreTestResultMatcher extends BaseMatcher<TestResult> {
        @Override
        public boolean matches(Object item) {
            return item instanceof IgnoreTestResult;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(IgnoreTestResult.class);
        }
    }

    private static class NoExceptedExceptionMatcher extends BaseMatcher<TestResult> {
        @Override
        public boolean matches(Object item) {
            return item instanceof NoExceptedExceptionTestResult;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(NoExceptedExceptionTestResult.class);
        }
    }
}