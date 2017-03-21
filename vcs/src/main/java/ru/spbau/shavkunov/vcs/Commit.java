package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Commit extends VcsObjectWithHash {
    private Date date;
    private String author;
    private String message;
    private String treeHash;

    public Commit(String author, String message, String treeHash, List<String> parentCommits, Repository repository)
                 throws IOException {
        this.date = new Date();
        this.author = author;
        this.message = message;
        this.treeHash = treeHash;

        byte[] content = getContent(parentCommits);
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    private byte[] getContent(List<String> parentCommits) throws IOException {
        byte[] res;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeObject(date);
            output.writeObject(author);
            output.writeObject(message);
            output.writeObject(treeHash);

            output.writeObject(parentCommits);
            output.writeObject(parentCommits);

            output.flush();
            res = byteArrayOutputStream.toByteArray();
        }

        return res;
    }

    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}