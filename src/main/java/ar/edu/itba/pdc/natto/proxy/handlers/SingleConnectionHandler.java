package ar.edu.itba.pdc.natto.proxy.handlers;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class SingleConnectionHandler extends ConnectionHandler {
    private ByteBuffer buffer;

    public SingleConnectionHandler(final Selector selector, final SocketChannel channel) {
        super(selector, channel, channel);
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        boolean validOps = false;
        boolean errorOccurred = false;

        if ((readyOps & SelectionKey.OP_READ) != 0) {
            validOps = true;
            buffer = buffers.acquire();
            errorOccurred = read(buffer) == -1;
        }

        // TODO: En else?
        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            validOps = true;
            errorOccurred = write(buffer) == -1;
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
            from.register(selector, SelectionKey.OP_READ, this);
        }

        return bytesWritten;
    }
}
