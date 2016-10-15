package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.tpe.io.ByteBufferPool;
import ar.edu.itba.pdc.tpe.io.CachedByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

@SuppressWarnings("Duplicates") // TODO: Remove
public class DualConnectionHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(DualConnectionHandler.class);

    private static final int BUFFER_SIZE = 1024;
    private static final ByteBufferPool buffers = new CachedByteBufferPool(BUFFER_SIZE);

    private final Selector selector;
    private final SocketChannel from;
    private final SocketChannel to;

    private ByteBuffer bufferRead;
    private ByteBuffer bufferWrite;

    public DualConnectionHandler(final Selector selector, final SocketChannel from)
            throws IOException {
        checkNotNull(selector, "Selector can't be null");
        checkArgument(selector.isOpen(), "Selector isn't open");

        checkNotNull(from, "Channel can't be null");
        checkArgument(from.isOpen(), "Channel isn't open");

        this.selector = selector;
        this.from = from;
        this.to = SocketChannel.open();
        to.configureBlocking(false);
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        boolean error = true;

        if ((readyOps & SelectionKey.OP_READ) != 0) {
            error = false;
            read();
        }

        // TODO: En else?
        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            error = false;
            write();
        }

        // TODO: En else?
        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
            error = false;
            connect();
        }

        checkState(!error);
    }

    private void read() throws IOException {
        int bytesRead;

        bufferRead = buffers.acquire();

        try {
            bytesRead = from.read(bufferRead);
        } catch (IOException exception) {
            // TODO: Close & Cancel diferente al del if (bytesRead == -1)?
            bytesRead = -1;
        }

        // The channel has reached end-of-stream or error
        if (bytesRead == -1) {
            // TODO: Close & Cancel & Logger
            closeConnection();
            return;
        }

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            bufferRead.flip();
            bufferWrite = bufferRead;
            bufferRead = null;

            // TODO: Remove
            System.out.println("Read: " + new String(bufferWrite.array(), bufferWrite.position(),
                    bufferWrite.limit(), Charset.forName("UTF-8")));

            from.register(selector, SelectionKey.OP_WRITE, this);
            // TODO: Parse & Change key ops
        }
    }

    private void write() throws IOException {
        // TODO: Improve
        int bytesWritten;

        try {
            bytesWritten = to.write(bufferWrite);
        } catch (IOException exception) {
            // TODO: Close & Cancel & Logger
            closeConnection();
            return;
        }

        if (!bufferWrite.hasRemaining()) {
            buffers.release(bufferWrite);
            bufferWrite = null;
            from.register(selector, SelectionKey.OP_READ, this); // TODO:
        }
    }

    private void connect() {
        try {
            // If this channel is in non-blocking mode then this method will return
            // false if the connection process is not yet complete.
            if (to.finishConnect()) {
                SocketAddress serverAddress = to.socket().getRemoteSocketAddress();

                logger.info("Established connection with server on " + serverAddress);

                // TODO: Change key ops
                from.register(selector, SelectionKey.OP_READ, this);
                to.register(selector, SelectionKey.OP_READ,
                        new DualConnectionHandler(selector, to));
            }
        } catch (IOException exception) {
            logger.error("Couldn't establish connection with server", exception);

            closeConnection();
        }
    }

    private void closeConnection() {
        // TODO: Mejorar
        checkNotNull(from.keyFor(selector)).cancel();
        checkNotNull(to.keyFor(selector)).cancel(); // TODO:

        try {
            from.close();
            to.close();
        } catch (IOException closeException) {
            logger.error("Can't properly closeConnection connection", closeException);
        }
    }
}
