package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.natto.dispatcher.ChannelOperation;
import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.io.Closeables;
import ar.edu.itba.pdc.natto.proxy.ProtocolTask;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketConnectionHandler<T> implements ConnectionHandler, Connection {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final int BUFFER_SIZE = 1024;

    private final DispatcherSubscriber subscriber;

    private final SocketChannel channel;
    private Connection connection;

    ProtocolTask<T> task;

    private Queue<ByteBuffer> messages;

    public SocketConnectionHandler(final SocketChannel channel,
                                   final DispatcherSubscriber subscriber) {
        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");

        this.subscriber = checkNotNull(subscriber, "Register can't be null");
        this.channel = channel;
        this.connection = this;
        this.messages = new ConcurrentLinkedQueue<>();
    }

    public void requestConnect(final InetSocketAddress serverAddress) throws IOException {
        checkState(connection == this);
        checkNotNull(serverAddress, "Address can't be null");
        checkArgument(!serverAddress.isUnresolved(), "Invalid address");

        // TODO: Sacar channel.getRemoteAddress() pq tira exception
        logger.info("Channel " + channel.getRemoteAddress() + " requested connection to: "
                + serverAddress);

        SocketChannel server = SocketChannel.open();
        server.configureBlocking(false);
        server.connect(serverAddress);

        SocketConnectionHandler serverHandler = new SocketConnectionHandler(server, subscriber);
        serverHandler.connection = this;
        this.connection = serverHandler;

        subscriber.unsubscribe(channel, ChannelOperation.READWRITE); // TODO:
        subscriber.subscribe(server, ChannelOperation.CONNECT, serverHandler);
    }

    @Override
    public void handle_connect() throws IOException {
        try {
            if (channel.finishConnect()) {
                // TODO: Sacar channel.getRemoteAddress() pq tira exception
                SocketAddress serverAddress = channel.socket().getRemoteSocketAddress();

                logger.info("Established connection with server on " + serverAddress);

                subscriber.unsubscribe(channel, ChannelOperation.CONNECT);
                subscriber.subscribe(channel, ChannelOperation.READ, this);
                connection.requestRead();
            }
        } catch (IOException exception) {
            logger.error("Couldn't establish connection with server", exception);

            logger.info("Closing connection with client");
            // TODO: Cerrar la otra conexion (? && Cerrar key?
            Closeables.closeSilently(channel);
        }
    }

    @Override
    public void requestRead() throws IOException {
        subscriber.subscribe(channel, ChannelOperation.READ, this);
    }

    @Override
    public void handle_read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE); // TODO: Pool
        int bytesRead;

        // TODO: Sacar channel.getRemoteAddress() pq tira exception
        logger.info("Channel " + channel.getRemoteAddress() + " requested read operation");

        if (connection == this) { // TODO: Remove!
            try {
                this.requestConnect(new InetSocketAddress(5222));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            return;
        }

        try {
            bytesRead = channel.read(buffer);
        } catch (IOException exception) {
            logger.error("Can't read channel channel", exception);

            // TODO: Cerrar la otra conexion (? && Cerrar key?
            Closeables.closeSilently(channel);

            return;
        }

        // The channel has reached end-of-stream or error
        if (bytesRead == -1) {
            logger.info("Channel reached EOF");
            // TODO: Cerrar ambas puntas?

            Closeables.closeSilently(channel);
            // TODO: Cerrar key?

            return;
        }

        // Cannot read more bytes than are immediately available
        if (bytesRead > 0) {
            buffer.flip();
            subscriber.unsubscribe(channel, ChannelOperation.READ);

            // TODO: Change
            try {
                System.out.println(new String(buffer.array(), buffer.position(), buffer.limit(),
                        Charset.forName("UTF-8")));

                connection.requestWrite(buffer);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void requestWrite(final ByteBuffer buffer) throws IOException {
        subscriber.subscribe(channel, ChannelOperation.WRITE, this);
        messages.offer(buffer);
    }

    @Override
    public void handle_write() throws IOException {
        if (messages.isEmpty()) {
            return;
        }

        ByteBuffer buffer = messages.peek();

        try {
            channel.write(buffer);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);

            Closeables.closeSilently(channel);
            // TODO: Cerrar la otra conexion (? && Cerrar key?

            return;
        }

        if (!buffer.hasRemaining()) {
            messages.remove();

            if (messages.isEmpty()) {
                subscriber.unsubscribe(channel, ChannelOperation.WRITE);
                connection.requestRead(); // TODO: Sacar (?
            }
        }
    }
}
