package ru.spbau.shavkunov;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 * Класс позволяющий создавать три различных ленивых вычисления.
 */
public class LazyFactory {
    /**
     * Создание ленивого вычисления в однопоточном режиме.
     * @param supplier вычисление, которое необходимо произвести.
     * @param <T> тип результата вычисления.
     * @return ленивое вычисление в однопоточном режиме.
     */
    public static <T> Lazy<T> createSingleLazy(Supplier<T> supplier) {
        return new SingleLazy<>(supplier);
    }

    /**
     * Класс реализующий ленивое вычисление в однопоточном режиме.
     * @param <T> тип результата вычисления.
     */
    private static class SingleLazy<T> implements Lazy<T> {
        private T result;
        private Supplier<T> supplier;

        public SingleLazy (Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * @see ru.spbau.shavkunov.Lazy
         */
        @Override
        public T get() {
            if (supplier != null) {
                result = supplier.get();
                supplier = null;
            }

            return result;
        }
    }

    /**
     * Создание ленивого вычисления в многопоточном режиме.
     * @param supplier вычисление, которое необходимо произвести.
     * @param <T> тип результата вычисления.
     * @return ленивое вычисление в многопоточном режиме.
     */
    public static <T> Lazy<T> createConcurrentLazy(Supplier<T> supplier) {
        return new ConcurrentLazy<>(supplier);
    }

    /**
     * Реализация ленивого вычисления с гарантией работы в многопоточном режиме.
     * @param <T> тип результата вычисления.
     */
    private static class ConcurrentLazy<T> implements Lazy<T> {
        private T result;
        private Supplier<T> supplier;

        public ConcurrentLazy (Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * @see ru.spbau.shavkunov.Lazy
         */
        @Override
        public T get() {
            if (supplier != null) {
                synchronized (this) {
                    if (supplier != null) {
                        result = supplier.get();
                        supplier = null;
                    }
                }
            }

            return result;
        }
    }

    /**
     * Создание lock free ленивого вычисления в многопоточном режиме.
     * @param <T> тип результата вычисления.
     * @param supplier вычисление, которое необходимо произвести.
     * @return ленивое вычисление в многопоточном режиме.
     */
    public static <T> Lazy<T> createLockFreeLazy(Supplier<T> supplier) {
        return new LockFreeLazy<>(supplier);
    }

    /**
     * Lock-free реализация ленивого вычисления с гарантией работы в многопоточном режиме.
     * @param <T> тип результата вычисления.
     */
    private static class LockFreeLazy<T> implements Lazy<T> {
        private static final Object marker = new Object();
        private Supplier<T> supplier;
        private volatile Object result = marker;

        private static final AtomicReferenceFieldUpdater<LockFreeLazy, Object> resultUpdater =
                AtomicReferenceFieldUpdater.newUpdater(LockFreeLazy.class, Object.class, "result");

        public LockFreeLazy (Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * @see ru.spbau.shavkunov.Lazy
         */
        @Override
        public T get() {
            if (result == marker) {
                // для зануления supplier, чтобы не было гонок на supplier.get()
                Supplier<T> localSupplierReference = supplier;
                if (localSupplierReference != null) {
                    resultUpdater.compareAndSet(this, marker, localSupplierReference.get());
                    supplier = null;
                }
            }

            return (T) result;
        }
    }
}