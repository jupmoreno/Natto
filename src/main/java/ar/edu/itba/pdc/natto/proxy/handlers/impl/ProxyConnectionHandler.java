package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import ar.edu.itba.pdc.natto.dispatcher.ChannelOperation;
import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.io.Closeables;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandler;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ProxyConnectionHandler implements ConnectionHandler, Connection {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);

    private static final int READ_BUFFER_SIZE = 1024;
    private static final int WRITE_BUFFER_SIZE = 10000;

    private final DispatcherSubscriber subscriber;

    private final SocketChannel channel;
    private ProtocolHandler handler;
    private Connection connection;

    private final ByteBuffer readBuffer;
    //    private final ByteBuffer writeBuffer; // TODO:
    private final Queue<ByteBuffer> messages; // TODO: Remove (?

    private boolean readRequested = false;
    private boolean closeRequested = false;
    private boolean alive = true;

    public ProxyConnectionHandler(final SocketChannel channel,
                                  final DispatcherSubscriber subscriber,
                                  final ProtocolHandler handler) {
        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");
        checkArgument(!channel.isBlocking(), "Channel is in blocking mode");

        this.subscriber = checkNotNull(subscriber, "Register can't be null");

        this.channel = channel;
        this.handler = checkNotNull(handler, "Handler can't be null");
        this.connection = this;

        this.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
//        this.writeBuffer = ByteBuffer.allocate(WRITE_BUFFER_SIZE); // TODO: // TODO: Size protocolo dependiente?
        this.messages = new ConcurrentLinkedQueue<>(); // TODO: Remove (?
    }

    @Override
    public void setHandler(ProtocolHandler handler) {
        this.handler = checkNotNull(handler, "Handler can't be null");
    }

    @Override
    public void requestConnect(final InetSocketAddress address,
                               final ProtocolHandler serverProtocol) throws IOException {
        checkNotNull(address, "Address can't be null");
        checkNotNull(serverProtocol, "Protocol handler can't be null");

        logger.info("Requested connection to: " + address);

        SocketChannel serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.connect(address);

        ProxyConnectionHandler serverHandler = new ProxyConnectionHandler(serverChannel, subscriber,
                serverProtocol);
        serverHandler.connection = this;
        this.connection = serverHandler;

        subscriber.subscribe(serverChannel, ChannelOperation.CONNECT, serverHandler);
    }

    @Override
    public void handle_connect() {
        try {
            if (channel.finishConnect()) {
                SocketAddress serverAddress = channel.socket().getRemoteSocketAddress();

                logger.info("Established connection with server on " + serverAddress);

                subscriber.unsubscribe(channel, ChannelOperation.CONNECT);

                handler.afterConnect(this, connection);
            }
        } catch (IOException exception) {
            logger.error("Couldn't establish connection with server", exception);

            forceClose();

//            // TODO: ?
//            logger.info("Closing connection with client");
//            connection.requestClose();
        }
    }

    @Override
    public void requestRead() {
        if (!channel.isConnectionPending()) {
            subscriber.subscribe(channel, ChannelOperation.READ, this);
        }
    }

    @Override
    public void handle_read() {
        int bytesRead;

        try {
            bytesRead = channel.read(readBuffer);
        } catch (IOException exception) {
            logger.error("Can't read from channel", exception);

            forceClose();

            return;
        }

        // The channel has reached end-of-stream
        if (bytesRead == -1) {
            logger.info("Channel reached EOF");

            // TODO: Is alive here?
            forceClose();

            return;
        }

        if (bytesRead > 0) {
            subscriber.unsubscribe(channel, ChannelOperation.READ);

            readBuffer.flip();
            handler.afterRead(this, connection, readBuffer);
            // TODO: buffer.clear(); (?.
        }
    }

    @Override
    public void requestWrite(ByteBuffer buffer) {
        if (!channel.isConnectionPending()) {
            subscriber.subscribe(channel, ChannelOperation.WRITE, this);
            messages.offer(buffer); // TODO: Aca o afuera del if?
        }
    }

    @Override
    public void handle_write() {
        checkState(!messages.isEmpty());

        ByteBuffer buffer = messages.peek();

        try {
            channel.write(buffer);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);

            forceClose();

            return;
        }

        if (!buffer.hasRemaining()) {
            messages.remove();

            if (messages.isEmpty()) {
                subscriber.unsubscribe(channel, ChannelOperation.WRITE);

                if (closeRequested) {
                    forceClose();
                } else {
                    handler.afterWrite(this, connection);
                }
            }
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void requestClose() {
        if (closeRequested) {
            return;
        }

        closeRequested = true;
        subscriber.unsubscribe(channel, ChannelOperation.READ);

        if (messages.isEmpty()) {
            handler.beforeClose(this, connection);
            Closeables.closeSilently(channel);
        }
    }

    private void forceClose() {
        alive = false;
        handler.beforeClose(this, connection);
        Closeables.closeSilently(channel);
    }
}
