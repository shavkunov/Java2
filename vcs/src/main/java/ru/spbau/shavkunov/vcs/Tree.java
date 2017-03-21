package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Tree extends VcsObjectWithHash {
    private TreeSet<ObjectWithName<Blob>> blobFiles;
    private TreeSet<ObjectWithName<Tree>> treeFiles;
    private byte[] content = null;

    public Tree(TreeSet<ObjectWithName<Blob>> blobFiles, TreeSet<ObjectWithName<Tree>> treeFiles) throws IOException {
        this.blobFiles = blobFiles;
        this.treeFiles = treeFiles;

        content = getContent();
        hash = DigestUtils.sha1Hex(content);
    }

    private byte[] getContent() throws IOException {
        if (content == null) {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

                serializeTreeSetNamedContent(output, blobFiles);
                serializeTreeSetNamedContent(output, treeFiles);
                output.flush();
                content = byteArrayOutputStream.toByteArray();
            }
        }

        return content;
    }

    @Override
    public void saveToStorage(Repository repository) throws IOException {
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    private void serializeTreeSetNamedContent(ObjectOutputStream output, TreeSet<? extends ObjectWithName> data)
                                              throws IOException {
        ArrayList<String> names = new ArrayList<>(data.stream()
                                                      .map(ObjectWithName::getName)
                                                      .collect(Collectors.toList()));

        output.writeObject(names);
    }


    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}
