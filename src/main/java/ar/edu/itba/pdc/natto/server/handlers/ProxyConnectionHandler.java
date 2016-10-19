package ar.edu.itba.pdc.natto.server.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.natto.server.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.io.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class ProxyConnectionHandler<T> implements ConnectionHandler, Connection {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final int BUFFER_SIZE = 1024;

    private final DispatcherSubscriber subscriber;

    private final SocketChannel from;
    private SocketChannel to;

    Queue<ByteBuffer> messages;

    public ProxyConnectionHandler(final DispatcherSubscriber subscriber, final SocketChannel from) {
        checkNotNull(from, "Channel can't be null");
        checkArgument(from.isOpen(), "Channel isn't open");

        this.subscriber = checkNotNull(subscriber, "Register can't be null");
        this.from = from;
        this.to = from;
    }

    public void requestConnect(final InetSocketAddress serverAddress) throws IOException {
        checkState(from.equals(to));
        checkNotNull(serverAddress, "Address can't be null");
        checkArgument(!serverAddress.isUnresolved(), "Invalid address");

        this.to = SocketChannel.open();
        this.to.configureBlocking(false);
        this.to.bind(serverAddress);

        subscriber.unsubscribe(from);
        subscriber.subscribe(to, SelectionKey.OP_CONNECT, this);
    }

    @Override
    public void handle_connect() throws IOException {
        try {
            if (to.finishConnect()) {
                SocketAddress serverAddress = to.socket().getRemoteSocketAddress();

                logger.info("Established connection with server on " + serverAddress);

                subscriber.subscribe(to, SelectionKey.OP_READ, getSwappedHandler(this));
                subscriber.subscribe(from, SelectionKey.OP_READ, this);
            }
        } catch (IOException exception) {
            logger.error("Couldn't establish connection with server", exception);

            logger.info("Closing connection with client");
            subscriber.unsubscribe(to);
            try {
                to.close();
                from.close();
            } catch (IOException closeException) {
                logger.error("Can't properly close connection", closeException);
            }
        }
    }

    @Override
    public void requestRead() throws IOException {
        subscriber.subscribe(from, SelectionKey.OP_READ, this);
    }

    @Override
    public void handle_read() throws IOException {
        ByteBuffer bufferRead = ByteBuffer.allocate(BUFFER_SIZE); // TODO: Pool
        int bytesRead;

        try {
            bytesRead = from.read(bufferRead);
        } catch (IOException exception) {
            logger.error("Can't read from channel", exception);

            Channels.closeSilently(from);
            subscriber.unsubscribe(from);
            Channels.closeSilently(to);
            subscriber.unsubscribe(to);

            return;
        }

        // The channel has reached end-of-stream or error
        if (bytesRead == -1) {
            logger.info("Channel reached EOF"); // ASK: Que significa?
            // TODO: Cerrar ambas puntas?

            Channels.closeSilently(from);
            subscriber.unsubscribe(from);

            return;
        }

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            bufferRead.flip();
//            checkState(task.add(bufferRead)); // TODO: Sacar
//            task.run(); // TODO: Sacar


            try {
                subscriber.subscribe(from, SelectionKey.OP_READ, this);
            } catch (ClosedChannelException exception) {
                // TODO:
            }
        }
    }

    @Override
    public boolean requestWrite(final ByteBuffer buffer) throws IOException {
        subscriber.subscribe(to, SelectionKey.OP_WRITE, this);
        return messages.offer(buffer); // TODO:
    }

    @Override
    public void handle_write() throws IOException {
        if (messages.isEmpty()) {
            return;
        }

        ByteBuffer buffer = messages.peek();

        try {
            to.write(buffer);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);

            Channels.closeSilently(to);
            subscriber.unsubscribe(to);
            Channels.closeSilently(from);
            subscriber.unsubscribe(from);

            return;
        }

        if (!buffer.hasRemaining()) {
            messages.remove();

            if (messages.isEmpty()) {
                subscriber.unsubscribe(to, SelectionKey.OP_WRITE); // TODO:
//                requestRead(); // TODO: Hacer ?
            }
        }
    }

    private static ProxyConnectionHandler getSwappedHandler(ProxyConnectionHandler old) {
        ProxyConnectionHandler handler = new ProxyConnectionHandler(old.subscriber, old.to);
        handler.to = old.from;

        return handler;
    }

//    @Override
//    public void accept(ByteBuffer buffer) {
//        messages.offer(buffer);
//        try {
//            subscriber.subscribe(to, SelectionKey.OP_WRITE, this);
//        } catch (ClosedChannelException e) {
//            e.printStackTrace();
//            // TODO:
//        }
//    }
}
