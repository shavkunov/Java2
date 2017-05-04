package ru.spbau.shavkunov.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.Scanner;

/**
 * ClI for client.
 */
public class Main {
    private static final String LIST_COMMAND = "list";
    private static final String GET_COMMAND = "get";
    private static final String DISCONNECT_COMMAND = "disconnect";

    /**
     * Parse arguments from console.
     * @param args user arguments.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected hostname and port: <host> <port>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        try (FileClient client = new FileClient(port, hostname);
             InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            client.connect();

            boolean isRunning = true;

            while (isRunning) {
                System.out.println("Enter command: ");
                String inputLine = bufferedReader.readLine();

                Scanner scanner = new Scanner(inputLine);

                if (!scanner.hasNext()) {
                    continue;
                }

                String command = scanner.next();

                switch (command) {
                    case LIST_COMMAND:
                        if (!scanner.hasNext()) {
                            System.out.println("Argument expected: <path to file>");
                            break;
                        }
                        break;

                    case GET_COMMAND:
                        if (!scanner.hasNext()) {
                            System.out.println("Argument excepted: <path to file on server>");
                            break;
                        }

                        String path = scanner.next();

                        client.executeGet(path);

                        break;

                    case DISCONNECT_COMMAND:
                        isRunning = false;
                        break;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            }

        } catch (ConnectException e) {
            System.out.println("Connect was unsuccessful");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Close connection error.");
        }
    }
}