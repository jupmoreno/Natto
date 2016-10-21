package ar.edu.itba.pdc.natto.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class Closeables {
    private static final Logger logger = LoggerFactory.getLogger(Closeables.class);

    public static void closeSilently(final Closeable channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException exception) {
            logger.error("Can't properly close connection", exception);
        }
    }

    public static void closeSilently(final Closeable channel, String message) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException exception) {
            logger.error(message, exception);
        }
    }
}
