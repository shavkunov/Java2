package ru.spbau.shavkunov;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Интерфейс, который объединяет объекты, которые умеют считать MD5 хеши.
 * (Название, я бы поменял)
 */
public interface HashComputing {
    /**
     * Вычисление MD5 хеша. Если вернулось null, то что-то пошло не так...
     * @return вычисленный хеш.
     * @throws NoSuchAlgorithmException исключение, которое бросает MessageDigest в случае несуществующего алгоритма
     * хеширования. В нашем случае он MD5, поэтому исключение не должно бросаться.
     * @throws IOException исключение, если возникли проблемы с чтением содержимого файлов.
     */
    byte[] getHash() throws NoSuchAlgorithmException, IOException;
}
