package ar.edu.itba.pdc.natto.proxy.handlers;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class DualConnectionHandler extends ConnectionHandler {
    private ByteBuffer buffer;

    public DualConnectionHandler(final Selector selector, final SocketChannel from)
            throws IOException {
        super(selector, from, SocketChannel.open());
        to.configureBlocking(false);
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        boolean validOps = false;
        boolean errorOccurred = false;

        if ((readyOps & SelectionKey.OP_READ) != 0) {
            validOps = true;
            buffer = buffers.acquire();
            errorOccurred = read(buffer) == -1 ? true : false;
        }

        // TODO: En else?
        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            validOps = true;
            errorOccurred = write(buffer) == -1 ? true : false;
        }

        // TODO: En else?
        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
            validOps = true;
            connect();
        }

        if (errorOccurred) {
            closeChannelAndCancelKey(from);
        }

        checkState(validOps);
    }

    @Override
    protected int read(ByteBuffer bufferRead) throws IOException {
        int bytesRead = super.read(bufferRead);

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            // TODO: Remove
            System.out.println("Read: " + new String(bufferRead.array(), bufferRead.position(),
                    bufferRead.limit(), Charset.forName("UTF-8")));

            // TODO: Parse & Change key ops
            from.register(selector, SelectionKey.OP_WRITE, this);
        }

        return bytesRead;
    }

    @Override
    protected int write(ByteBuffer bufferWrite) throws IOException {
        int bytesWritten = super.write(bufferWrite);

        // TODO: Sacar el != -1 (?
        if (bytesWritten != -1 && !bufferWrite.hasRemaining()) {
            // TODO: Change key ops
            to.register(selector, SelectionKey.OP_READ, this);
        }

        return bytesWritten;
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

            closeChannelAndCancelKey(from);
            closeChannelAndCancelKey(to);
        }
    }
}
