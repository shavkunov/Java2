package ru.spbau.shavkunov;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.RecursiveTask;

/**
 * Вычисление MD5 хеша файла с помощью ForkJoinPool.
 */
public class ConcurrentHashComputing implements HashComputing {
    /**
     * Путь к файлу.
     */
    private @NotNull Path path;

    public ConcurrentHashComputing(@NotNull Path path) {
        this.path = path;
    }

    @Override
    public byte[] getHash() throws NoSuchAlgorithmException, IOException {
        GetHashTask task = new GetHashTask(path);
        return task.compute();
    }

    /**
     * Задача, которая вычисляет хеш файла.
     * Задача считается маленькой, если нужно вычислить хеш регулярного файла.
     */
    private class GetHashTask extends RecursiveTask<byte[]> {
        /**
         * Путь к файлу.
         */
        private @NotNull Path path;

        public GetHashTask(@NotNull Path path) {
            this.path = path;
        }

        @Override
        protected byte[] compute() {
            if (path.toFile().isFile()) {
                SimpleHashComputing hash = new SimpleHashComputing(path);
                try {
                    return hash.getHash();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(path.toFile().getName().getBytes());
                    LinkedList<GetHashTask> tasks = new LinkedList<>();

                    for (File file : path.toFile().listFiles()) {
                        GetHashTask newTask = new GetHashTask(file.toPath());
                        newTask.fork();
                        tasks.add(newTask);
                    }

                    for (GetHashTask task : tasks) {
                        byte[] taskRes = task.join();
                        md.update(taskRes);
                    }

                    return md.digest();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}
