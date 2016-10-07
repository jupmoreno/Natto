package ar.edu.itba.pdc.tpe.proxy.handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class IOHandler implements Handler {
    private final Selector selector;
    private final SocketChannel from;
    private final SocketChannel to;
    private ByteBuffer buffer = ByteBuffer.allocate(50);

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

        try {
            bytesRead = from.read(buffer);
        } catch (IOException e) {
            bytesRead = -1;
        }

        if (bytesRead == -1) {
            // TODO: Close
            return;
        }

        buffer.flip();
        from.register(selector, SelectionKey.OP_WRITE, this); // TODO:
    }

    private void write() throws IOException {
        // TODO: Improve

        try {
            to.write(buffer);
        } catch (IOException e) {
            // TODO: Remove
            // This should never happen.
            e.printStackTrace();
        }

        if (buffer.remaining() == 0) {
            buffer.clear();
            from.register(selector, SelectionKey.OP_READ, this); // TODO:
        }
    }
}
