package ru.spbau.shavkunov.vcs;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static ru.spbau.shavkunov.vcs.Constants.OBJECTS_FOLDER;

public class Tree extends VcsObjectWithHash {
    private Set<ObjectWithName<Blob>> blobFiles;
    private Set<Tree> treeFiles;

    public Tree(Set<ObjectWithName<Blob>> blobFiles, Set<Tree> treeFiles, Repository repository)
                throws IOException {
        this.blobFiles = blobFiles;
        this.treeFiles = treeFiles;

        byte[] content = getContent();
        hash = DigestUtils.sha1Hex(content);
        Files.write(repository.getObjectsPath().resolve(hash), content);
    }

    public Tree() {
        blobFiles = new TreeSet<>();
        treeFiles = new TreeSet<>();
    }

    private byte[] getContent() throws IOException {
        byte[] content;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            output.writeObject(new ArrayList<>(blobFiles.stream()
                                                        .map(ObjectWithName::getName)
                                                        .collect(Collectors.toList())));
            output.writeObject(treeFiles);
            output.flush();
            content = byteArrayOutputStream.toByteArray();
        }

        return content;
    }

    public void addBlob(Blob blob, String name) {
        blobFiles.add(new ObjectWithName<>(blob, name));
    }

    public void addChild(Tree tree) {
        treeFiles.add(tree);
    }


    @Override
    public Path getPathToObject(Repository repository) {
        return repository.getRootDirectory().resolve(OBJECTS_FOLDER).resolve(hash);
    }
}