package ru.spbau.shavkunov.ftp.handlers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.shavkunov.ftp.message.FileWriter;
import ru.spbau.shavkunov.ftp.message.Message;
import ru.spbau.shavkunov.ftp.message.MessageWriter;
import ru.spbau.shavkunov.ftp.message.Writer;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

public class GetQueryHandler {
    private static final @NotNull Logger logger = LoggerFactory.getLogger(GetQueryHandler.class);
    @NotNull Path path;

    public GetQueryHandler(@NotNull Path path) {
        this.path = path;
    }

    public @NotNull Writer handleGetQuery(@NotNull WritableByteChannel channel) throws IOException {
        logger.debug("Handling get task");

        if (path.toFile().isDirectory()) {
            byte [] zeroInformationArray = { 0x00 };
            Message emptyMessage = new Message(zeroInformationArray);
            return new MessageWriter(emptyMessage, channel);
        }

        return new FileWriter(path, channel);
    }
}