package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.tpe.buffers.ByteBufferPool;
import ar.edu.itba.pdc.tpe.buffers.CachedByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class IOHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(IOHandler.class);

    private static final int BUFFER_SIZE = 1024;
    private static final ByteBufferPool buffers = new CachedByteBufferPool(BUFFER_SIZE);

    private final Selector selector;
    private final SocketChannel from;
    private final SocketChannel to;

    private ByteBuffer bufferRead;
    private ByteBuffer bufferWrite;

    public IOHandler(final Selector selector, final SocketChannel from,
                     final SocketChannel to) {
        checkNotNull(selector, "Null selector");
        checkArgument(selector.isOpen(), "Invalid selector");
        checkNotNull(from, "Null from channel");
        checkArgument(from.isOpen(), "Invalid from channel");
        checkNotNull(to, "Null to channel");
        checkArgument(to.isOpen(), "Invalid to channel");

        this.selector = selector;
        this.from = from;
        this.to = to;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        boolean error = true;

        if ((readyOps & SelectionKey.OP_READ) != 0) {
            error = false;
            read();
        } else if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            error = false;
            write();
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
            // TODO: Close & Cancel
            checkNotNull(from.keyFor(selector), "No registered key").cancel();
            from.close();
            checkNotNull(to.keyFor(selector), "No registered key").cancel();
            to.close();
            return;
        }

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            bufferRead.flip();
            bufferWrite = bufferRead;
            bufferRead = null;

            from.register(selector, SelectionKey.OP_WRITE, this);
            // TODO: Parse & Change key ops
        }
    }

    private void write() throws IOException {
        // TODO: Improve

        try {
            to.write(bufferWrite);
        } catch (IOException exception) {
            // TODO:
            // This should never happen.
            exception.printStackTrace(); // TODO: Remove
            checkState(false); // TODO: Remove
        }

        if (bufferWrite.remaining() == 0) {
            buffers.release(bufferWrite);
            bufferWrite = null;
            from.register(selector, SelectionKey.OP_READ, this); // TODO:
        }
    }
}
