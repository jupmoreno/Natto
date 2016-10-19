package ar.edu.itba.pdc.natto.server.io;

import ar.edu.itba.pdc.natto.server.handlers.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class Channels {
    private static final Logger logger = LoggerFactory.getLogger(Channels.class);

    public static void closeSilently(final SocketChannel channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException exception) {
            logger.error("Can't properly close connection", exception);
        }
    }

    public static void closeSilently(final SocketChannel channel, String message) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException exception) {
            logger.error(message, exception);
        }
    }
}
