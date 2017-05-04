package ru.spbau.shavkunov.ftp;

public class FtpTest {
    /*private static Server server;
    private static Client client;

    static {
        try {
            server = new FileServer(PORT);
            server.start();
            client = new FileClient(PORT, hostname);
            client.connect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() {} // Empty method to trigger the execution of the block above

    @Test(expected = ConnectException.class)
    public void initTest() throws IOException, ConnectException {
        Client wrongClient = new FileClient(PORT + 1, hostname);
        wrongClient.connect();
    }

    @Test(expected = FileNotExistsException.class)
    public void listOfInvalidFile() throws FileNotExistsException {
        client.executeList("thisPathDoesn'tExist");
    }

    @Test(expected = FileNotExistsException.class)
    public void getOfInvalidFile() throws FileNotExistsException {
        client.executeGet("WhatPathIsthis?");
    }

    @Test
    public void executeListTest() throws FileNotExistsException {
        String path = "test";

        Map<String, Boolean> rightAnswer = new HashMap<String, Boolean>();
        rightAnswer.put("dir", true);
        rightAnswer.put("test1", false);
        rightAnswer.put("test2", false);

        Optional<Map<String, Boolean>> serverResponse = client.executeList(path);

        assertThat(rightAnswer, is(serverResponse.get()));
        assertEquals(rightAnswer.size(), serverResponse.get().size());
    }

    @Test
    public void executeGetTest() throws FileNotExistsException, IOException {
        String path = "test/test2";

        Optional<File> response = client.executeGet(path);
        File file = response.get();

        assertTrue(file.exists());
        byte[] content = Files.readAllBytes(standardDownloadsFolder.resolve("test2"));
        String line = new String(content);

        assertEquals("text2", line);
    }

    @Test
    public void executeGetOnBigFile() throws IOException, FileNotExistsException {
        Path path = Paths.get(".").resolve("test").resolve("tmpFile");
        String tmpFile = "test/tmpFile";

        Files.createFile(path);

        byte[] bigContent = new byte[10_000_000];
        for (int i = 0; i < bigContent.length; i++) {
            bigContent[i] = 1;
        }

        Files.write(path, bigContent);

        client.executeGet("test/tmpFile");

        assertTrue(standardDownloadsFolder.resolve("tmpFile").toFile().exists());
        assertEquals(10_000_000, standardDownloadsFolder.resolve("tmpFile").toFile().length());

        path.toFile().delete();
    }*/
}
