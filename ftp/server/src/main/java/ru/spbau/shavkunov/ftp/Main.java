package ru.spbau.shavkunov.ftp;

import java.io.IOException;

import static ru.spbau.shavkunov.ftp.NetworkConstants.PORT;

public class Main {
    public static void main(String[] args) {
        try {
            Server server = new FileServer(PORT);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}