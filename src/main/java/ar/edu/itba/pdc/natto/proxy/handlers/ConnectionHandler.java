package ar.edu.itba.pdc.natto.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.io.ByteBufferPool;
import ar.edu.itba.pdc.natto.io.CachedByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class ConnectionHandler implements SelectorHandler {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private static final int BUFFER_SIZE = 1024;
    // TODO: En otro lado
    protected static final ByteBufferPool buffers = new CachedByteBufferPool(BUFFER_SIZE);

    protected final Selector selector;
    protected final SocketChannel from;
    protected final SocketChannel to;

    public ConnectionHandler(final Selector selector, final SocketChannel from,
                             final SocketChannel to) {
        checkNotNull(selector, "Selector can't be null");
        checkArgument(selector.isOpen(), "Selector isn't open");

        checkNotNull(from, "Channel can't be null");
        checkArgument(from.isOpen(), "Channel isn't open");

        checkNotNull(to, "Channel can't be null");
        checkArgument(to.isOpen(), "Channel isn't open");

        this.selector = selector;
        this.from = from;
        this.to = to;
    }

    protected int read(ByteBuffer bufferRead) throws IOException {
        int bytesRead;

        try {
            bytesRead = from.read(bufferRead);
        } catch (IOException exception) {
            logger.error("Can't read from channel", exception);
            throw exception;
        }

        // The channel has reached end-of-stream or error
        if (bytesRead == -1) {
            logger.error("Channel reached EOF"); // ASK: Que significa?
            return -1;
        }

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            bufferRead.flip();
        }

        return bytesRead;
    }

    protected int write(ByteBuffer bufferWrite) throws IOException {
        // TODO: Improve
        int bytesWritten;

        try {
            bytesWritten = to.write(bufferWrite);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);
            throw exception;
        }

        return bytesWritten;
    }

    protected void closeChannelAndCancelKey(SocketChannel channel) {
        SelectionKey key = channel.keyFor(selector);
        if (key != null) {
            key.cancel();
        }

        closeChannel(channel);
    }

    protected void closeChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException closeException) {
            logger.error("Can't properly close connection", closeException);
        }
    }
}
