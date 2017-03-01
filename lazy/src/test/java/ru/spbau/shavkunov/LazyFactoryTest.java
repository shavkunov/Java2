package ru.spbau.shavkunov;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.function.Supplier;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LazyFactoryTest {
    private int counter;
    private String testMessage = "Supplier result";
    private Supplier supplier = new Supplier() {
        @Override
        public String get() {
            counter++;
            return testMessage;
        }
    };

    private Supplier nullSupplier = new Supplier() {
        @Override
        public Object get() {
            counter++;
            return null;
        }
    };

    @Before
    public void setUp() throws Exception {
        counter = 0;
    }

    @Test
    public void singleLazyTest() {
        assertEquals(0, counter);
        Lazy lazyWithString = LazyFactory.createSingleLazy(supplier);
        String actualResult = (String) lazyWithString.get();
        assertEquals(testMessage, actualResult);
        assertEquals(1, counter);
        assertSame(actualResult, lazyWithString.get());
        assertEquals(1, counter);
    }

    @Test
    public void singleLazyTestWithNullSupplier() {
        Lazy lazy = LazyFactory.createSingleLazy(nullSupplier);
        assertNull(lazy.get());
    }

    private void concurrentTest(Lazy concurrentLazy, String example, boolean isLockFree) throws InterruptedException {
        ArrayList<Thread> threads = new ArrayList<>();
        int threadsAmount = 10;
        String[] res = new String[threadsAmount];
        for (int i = 0; i < threadsAmount; i++) {
            int finalI = i;
            threads.add(new Thread(() -> res[finalI] = (String) concurrentLazy.get()));
        }

        threads.forEach(Thread::start);

        for (Thread thread : threads) {
            thread.join();
        }

        if (isLockFree) {
            assertTrue(counter >= 1);
        } else {
            assertEquals(1, counter);
        }
        for (String result : res) {
            assertEquals(result, example);
            assertSame(res[0], result);
        }
    }

    private void concurrentNotNullTest(Lazy concurrentLazy, boolean isLockFree) throws InterruptedException {
        concurrentTest(concurrentLazy, testMessage, isLockFree);
    }

    private void concurrentNullTest(Lazy concurrentLazy, boolean isLockFree) throws InterruptedException {
        concurrentTest(concurrentLazy, null, isLockFree);
    }

    @Test
    public void concurrentLazyTest() throws InterruptedException {
        Lazy concurrentLazy = LazyFactory.createConcurrentLazy(supplier);
        Lazy concurrentLazyWithNullSupplier = LazyFactory.createConcurrentLazy(nullSupplier);
        concurrentNotNullTest(concurrentLazy, false);
        counter = 0;
        concurrentNullTest(concurrentLazyWithNullSupplier, false);
    }

    @Test
    public void lockFreeLazyTest() throws InterruptedException {
        Lazy lockFreeLazy = LazyFactory.createLockFreeLazy(supplier);
        Lazy lazyWithNullSupplier = LazyFactory.createLockFreeLazy(nullSupplier);
        concurrentNotNullTest(lockFreeLazy, true);
        counter = 0;
        concurrentNullTest(lazyWithNullSupplier, true);
    }
}