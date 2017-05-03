package ru.spbau.shavkunov.ftp.handlers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.message.FileWriter;
import ru.spbau.shavkunov.ftp.message.MessageWriter;
import ru.spbau.shavkunov.ftp.message.Writer;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

import static ru.spbau.shavkunov.ftp.NetworkConstants.EMPTY_MESSAGE;

/**
 * Class handles get task.
 */
public class GetQueryHandler {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(GetQueryHandler.class);

    /**
     * Path to file, which is need to send to client.
     */
    private @NotNull Path path;

    /**
     * Creating handler.
     * @param path path to file, which will be sent to client.
     */
    public GetQueryHandler(@NotNull Path path) {
        this.path = path;
    }

    /**
     * Creating writer.
     * @param channel client channel
     * @return writer, which is ready for send messages to client.
     * Writer will send zero message if path is path to directory or file doesn't exist.
     * @throws IOException if an I/O error occurs.
     */
    public @NotNull Writer handleGetQuery(@NotNull WritableByteChannel channel) throws IOException {
        logger.debug("Handling get task");

        if (path.toFile().isDirectory() || !path.toFile().exists()) {
            return new MessageWriter(EMPTY_MESSAGE, channel);
        }

        return new FileWriter(path, channel);
    }
}