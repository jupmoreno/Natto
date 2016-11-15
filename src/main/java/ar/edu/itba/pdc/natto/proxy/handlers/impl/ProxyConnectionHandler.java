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

public class ProxyConnectionHandler implements ConnectionHandler, Connection {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);

    private static final int READ_BUFFER_SIZE = 1024;
    private static final int WRITE_BUFFER_SIZE = 1024;

    private final DispatcherSubscriber subscriber;

    private final SocketChannel channel;
    private ProtocolHandler handler;

    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    private boolean closeRequested = false;

    public ProxyConnectionHandler(final SocketChannel channel,
                                  final DispatcherSubscriber subscriber,
                                  final ProtocolHandler handler) {
        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");
        checkArgument(!channel.isBlocking(), "Channel is in blocking mode");

        this.subscriber = checkNotNull(subscriber, "Register can't be null");

        this.channel = channel;
        this.handler = checkNotNull(handler, "Handler can't be null");

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
    public boolean requestConnect(final InetSocketAddress address,
                                  final ProtocolHandler serverProtocol) {
        checkNotNull(address, "Address can't be null");
        checkNotNull(serverProtocol, "Protocol handler can't be null");

        logger.info("Requested connection to " + address);

        SocketChannel serverChannel = null;

        try {
            serverChannel = SocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.connect(address);
        } catch (IOException exception) {
            logger.error("Can't initiate connection to server", exception);

            if (serverChannel != null) {
                Closeables.closeSilently(serverChannel);
            }

            return false;
        }

        ProxyConnectionHandler serverHandler = new ProxyConnectionHandler(serverChannel, subscriber,
                serverProtocol);

        subscriber.subscribe(serverChannel, ChannelOperation.CONNECT, serverHandler);

        return true;
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
        }
    }

    @Override
    public boolean requestRead() {
        if (closeRequested || channel.isConnectionPending() || !channel.isOpen()) {
            return false;
        }

        subscriber.subscribe(channel, ChannelOperation.READ, this);

        return true;
    }

    @Override
    public void handle_read() {
        int bytesRead;

        try {
            bytesRead = channel.read(readBuffer);
        } catch (IOException exception) {
            logger.error("Can't read from channel, closing...", exception);

            forceClose();

            return;
        }

        // The channel has reached end-of-stream
        if (bytesRead == -1) {
            logger.warn("Channel reached EOF, closing");

            forceClose();

            return;
        }

        if (bytesRead > 0) {
            subscriber.unsubscribe(channel, ChannelOperation.READ);

            readBuffer.flip();
            handler.afterRead(readBuffer);
            readBuffer.clear();
        }
    }

    @Override
    public boolean requestWrite(ByteBuffer source) {
        if (channel.isConnectionPending() || closeRequested || !channel.isOpen()) {
            return false;
        }

        while (writeBuffer.position() < writeBuffer.limit() && source.hasRemaining()) {
            writeBuffer.put(source.get());
        }

        subscriber.subscribe(channel, ChannelOperation.WRITE, this);

        return true;
    }

    @Override
    public void handle_write() {
        writeBuffer.flip();

        try {
            channel.write(writeBuffer);
        } catch (IOException exception) {
            logger.error("Can't write to channel", exception);

            forceClose();

            return;
        }

        if (!writeBuffer.hasRemaining()) {
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
        return channel.isOpen();
    }

    @Override
    public void requestClose() {
        if (closeRequested || !channel.isOpen()) {
            return;
        }

        closeRequested = true;
        subscriber.unsubscribe(channel, ChannelOperation.READ);

        if (writeBuffer.position() == 0) {
            forceClose();
        }
    }

    private void forceClose() {
        handler.beforeClose();
        subscriber.cancel(channel);
        Closeables.closeSilently(channel);
    }
}
