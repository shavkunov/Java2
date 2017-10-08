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

import static ru.spbau.shavkunov.ftp.NetworkConstants.EMPTY_MESSAGE;

/**
 * Class handles list task.
 */
public class ListQueryHandler {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(ListQueryHandler.class);

    /**
     * path to directory
     */
    private @NotNull Path path;

    /**
     * Creating handler.
     * @param path path to directory, where list task will be executed.
     */
    public ListQueryHandler(@NotNull Path path) {
        logger.debug("List handler created with path : {}", path);
        this.path = path;
    }

    /**
     * Creating writer.
     * @param channel client channel.
     * @return writer, which ready to send message of serialized list data.
     * It will send zero message if path is directory or file doesn't exist.
     */
    public @NotNull MessageWriter handleListQuery(@NotNull WritableByteChannel channel) {
        if (!path.toFile().exists() || !path.toFile().isDirectory()) {
            return new MessageWriter(EMPTY_MESSAGE, channel);
        }

        byte[] content = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(byteArrayOutputStream)) {

            if (path.toFile().exists() && path.toFile().isDirectory()) {
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