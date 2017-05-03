package ru.spbau.shavkunov.ftp;

import org.jetbrains.annotations.NotNull;
import ru.spbau.shavkunov.ftp.message.Message;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class of global constants.
 */
public class NetworkConstants {
    /**
     * port which server are listen to.
     */
    public static int PORT = 12342;

    /**
     * Local server name.
     */
    public static @NotNull String hostname = "localhost";

    /**
     * list query identifier.
     */
    public static final int LIST_QUERY = 1;

    /**
     * get query identifier.
     */
    public static final int GET_QUERY = 2;

    /**
     * Downloads folder in user System.
     */
    public static final @NotNull Path standardDownloadsFolder = Paths.get(System.getProperty("user.home")).resolve("Downloads");

    /**
     * Zero byte array representing lack of information.
     */
    private static byte [] zeroInformationArray = { 0x00 };

    /**
     * Empty message for special cases.
     */
    public static Message EMPTY_MESSAGE = new Message(zeroInformationArray);
}