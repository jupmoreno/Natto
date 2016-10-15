package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.tpe.io.ByteBufferPool;
import ar.edu.itba.pdc.tpe.io.CachedByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

@SuppressWarnings("Duplicates") // TODO: Remove
public class SingleConnectionHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(SingleConnectionHandler.class);

    private static final int BUFFER_SIZE = 1024;
    // TODO: En otro lado
    private static final ByteBufferPool buffers = new CachedByteBufferPool(BUFFER_SIZE);

    private final Selector selector;
    private final SocketChannel channel;

    private ByteBuffer bufferRead;
    private ByteBuffer bufferWrite = bufferRead;

    public SingleConnectionHandler(final Selector selector, final SocketChannel channel) {
        checkNotNull(selector, "Selector can't be null");
        checkArgument(selector.isOpen(), "Selector isn't open");

        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");

        this.selector = selector;
        this.channel = channel;
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

        checkState(!error);
    }

    private void read() throws IOException {
        int bytesRead;

        bufferRead = buffers.acquire();

        try {
            bytesRead = channel.read(bufferRead);
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

            // TODO: Parse & Change key ops
            channel.register(selector, SelectionKey.OP_WRITE, this);
        }
    }

    private void write() throws IOException {
        // TODO: Improve
        int bytesWritten;

        try {
            bytesWritten = channel.write(bufferWrite);
        } catch (IOException exception) {
            // TODO: Close & Cancel & Logger
            closeConnection();
            return;
        }

        if (!bufferWrite.hasRemaining()) {
            buffers.release(bufferWrite);
            bufferWrite = null;
            channel.register(selector, SelectionKey.OP_READ, this); // TODO:
        }
    }

    private void closeConnection() {
        checkNotNull(channel.keyFor(selector)).cancel();

        try {
            channel.close();
        } catch (IOException closeException) {
            logger.error("Can't properly close connection", closeException);
        }
    }
}
