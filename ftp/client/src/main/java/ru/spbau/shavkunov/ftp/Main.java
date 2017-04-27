package ru.spbau.shavkunov.ftp;

import ru.spbau.shavkunov.ftp.exceptions.UnknownException;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new FileClient(NetworkConstants.PORT, NetworkConstants.hostname);
            client.connect();
            String testFile = "test";
            Map<String, Boolean> list = client.executeList(testFile);
            for (String filename : list.keySet()) {
                System.out.println(filename + " isDir : " + list.get(filename));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnknownException e) {
            e.printStackTrace();
        }
    }
}