package ar.edu.itba.pdc.tpe.proxy.handlers;

import ar.edu.itba.pdc.tpe.buffers.ByteBufferPool;
import ar.edu.itba.pdc.tpe.buffers.CachedByteBufferPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class IOHandler implements Handler {
    private static final int BUFFER_SIZE = 1024;
    private static final ByteBufferPool buffers = new CachedByteBufferPool(BUFFER_SIZE);

    private final Selector selector;
    private final SocketChannel from;
    private final SocketChannel to;

    private ByteBuffer bufferRead;
    private ByteBuffer bufferWrite;

    public IOHandler(Selector selector, SocketChannel from, SocketChannel to) {
        this.selector = selector;
        this.from = from;
        this.to = to;
    }

    @Override
    public void handle(int readyOps) throws IOException {
        if ((readyOps & SelectionKey.OP_READ) != 0) {
            read();
        }

        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            write();
        }
    }

    private void read() throws IOException {
        // TODO: Improve
        int bytesRead = 0;
        bufferRead = buffers.adquire();

        try {
            bytesRead = from.read(bufferRead);
        } catch (IOException e) {
            bytesRead = -1;
        }

        if (bytesRead == -1) {
            // TODO: Close
            return;
        }

        bufferRead.flip();
        bufferWrite = bufferRead;
        bufferRead = null;
        from.register(selector, SelectionKey.OP_WRITE, this); // TODO:
    }

    private void write() throws IOException {
        // TODO: Improve

        try {
            to.write(bufferWrite);
        } catch (IOException e) {
            // TODO: Remove
            // This should never happen.
            e.printStackTrace();
        }

        if (bufferWrite.remaining() == 0) {
            buffers.release(bufferWrite);
            from.register(selector, SelectionKey.OP_READ, this); // TODO:
        }
    }
}
