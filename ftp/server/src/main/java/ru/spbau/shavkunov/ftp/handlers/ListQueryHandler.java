package ru.spbau.shavkunov.ftp.handlers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.message.Message;
import ru.spbau.shavkunov.ftp.message.MessageWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

public class ListQueryHandler {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(ListQueryHandler.class);
    private @NotNull Path path;

    public ListQueryHandler(@NotNull Path path) {
        logger.debug("List handler created with path : {}", path);
        this.path = path;
    }

    public @NotNull MessageWriter handleListQuery(@NotNull WritableByteChannel channel) {
        byte[] content = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            if (path.toFile().exists()) {
                int size = path.toFile().listFiles().length;
                logger.debug("Files in directory : {}", size);
                output.writeInt(size);
                for (File file : path.toFile().listFiles()) {
                    logger.debug("Writing a file : {}, {}", file.getName(), file.isDirectory());
                    output.writeObject(file.getName());
                    output.writeBoolean(file.isDirectory());
                }
            } else {
                logger.debug("that file doesn't exist");
                output.writeInt(0);
            }

            output.flush();
            content = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (content == null) {
            throw new InternalError();
        }

        Message message = new Message(content);
        MessageWriter writer = new MessageWriter(message, channel);

        return writer;
    }
}