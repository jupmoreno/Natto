package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import ar.edu.itba.pdc.natto.dispatcher.ChannelOperation;
import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.io.Closeables;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ProxyConnectionHandler implements ConnectionHandler, Connection {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);

    private static final int READ_BUFFER_SIZE = 1024;
    private static final int WRITE_BUFFER_SIZE = 1024;

    private final DispatcherSubscriber subscriber;

    private final SocketChannel channel;
    private ProtocolHandler handler;
    private Connection connection;

    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

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
        this.writeBuffer = ByteBuffer.allocate(WRITE_BUFFER_SIZE);

        handler.setConnection(this);
    }

    @Override
    public void setHandler(ProtocolHandler handler) {
        this.handler = checkNotNull(handler, "Handler can't be null");
        this.handler.setConnection(this);
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

                handler.afterConnect();
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
        checkState(channel.isOpen()); // TODO: Remove (jp)

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
            checkState(channel.isOpen()); // TODO: Remove (jp)
            subscriber.unsubscribe(channel, ChannelOperation.READ);

            readBuffer.flip();
            handler.afterRead(readBuffer);
            readBuffer.clear();
        }
    }

    @Override
    public void requestWrite(ByteBuffer source) {
        if (!channel.isConnectionPending()) {

            while (writeBuffer.position() < writeBuffer.limit() && source.hasRemaining()) {
                writeBuffer.put(source.get());
            }

            checkState(channel.isOpen()); // TODO: Remove (jp)
            subscriber.subscribe(channel, ChannelOperation.WRITE, this);
        }
    }

    @Override
    public void handle_write() {
        checkState(writeBuffer.position() != 0);

        writeBuffer.flip();

        try {
            channel.write(writeBuffer);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);

            forceClose();

            return;
        }

        if (!writeBuffer.hasRemaining()) {
            checkState(channel.isOpen()); // TODO: Remove (jp)
            subscriber.unsubscribe(channel, ChannelOperation.WRITE);

            if (closeRequested) {
                forceClose();
                return;
            }
        }

        writeBuffer.compact();

        handler.afterWrite();
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
        checkState(channel.isOpen()); // TODO: Remove (jp)
        subscriber.unsubscribe(channel, ChannelOperation.READ);

        if (writeBuffer.position() == 0) {
            handler.beforeClose();
            Closeables.closeSilently(channel);
        }
    }

    private void forceClose() {
        alive = false;
        handler.beforeClose();
        Closeables.closeSilently(channel);
    }
}
