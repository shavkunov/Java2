package ru.spbau.shavkunov;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Вычисление MD5 хеша в однопоточном режиме.
 */
public class SimpleHashComputing implements HashComputing {
    /**
     * Данные считываются с шагом равным этому числу.
     */
    private static int BLOCK_SIZE = 2048;

    /**
     * Путь к файлу.
     */
    private @NotNull Path path;

    public SimpleHashComputing(Path path) {
        this.path = path;
    }

    @Override
    public byte[] getHash() throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        if (path.toFile().isDirectory()) {
            md.update(path.toFile().getName().getBytes());
        } else {
            return getFileHash().digest();
        }

        for (File file : path.toFile().listFiles()) {
            SimpleHashComputing hash = new SimpleHashComputing(file.toPath());
            md.update(hash.getHash());
        }

        return md.digest();
    }

    /**
     * Непосредственно получение хеша файла.
     * @return инстанс Message Digest с хешом файла.
     * @throws IOException исключение, если возникли проблемы с чтением файлов.
     * @throws NoSuchAlgorithmException исключение, которое бросает MessageDigest в случае несуществующего алгоритма
     * хеширования. В нашем случае он MD5, поэтому исключение не должно бросаться.
     */
    private MessageDigest getFileHash() throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(path.toFile());
        byte[] bytes = new byte[BLOCK_SIZE];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            md.update(bytes, 0, numBytes);
        }

        return md;
    }
}