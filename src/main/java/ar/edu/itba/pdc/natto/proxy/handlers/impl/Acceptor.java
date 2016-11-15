package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.dispatcher.ChannelOperation;
import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.io.Closeables;
import ar.edu.itba.pdc.natto.proxy.handlers.AcceptHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Acceptor implements AcceptHandler {
    private static final Logger logger = LoggerFactory.getLogger(Acceptor.class);

    private final DispatcherSubscriber subscriber;
    private final ConnectionHandlerFactory handlers;
    private final ServerSocketChannel channel;

    public Acceptor(final ServerSocketChannel channel, final DispatcherSubscriber subscriber,
                    final ConnectionHandlerFactory handlers) {
        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");
        checkArgument(!channel.isBlocking(), "Channel is in blocking mode");

        this.channel = channel;
        this.subscriber = checkNotNull(subscriber, "Register can't be null");
        this.handlers = checkNotNull(handlers, "Handler factory can't be null");
    }

    @Override
    public void handle_accept() throws IOException {
        SocketChannel client;

        try {
            // Will immediately return null if there are no pending connections
            client = channel.accept();
        } catch (IOException exception) {
            logger.error("Channel can't accept new client", exception);
            throw exception;
        }

        try {
            if (client != null) {
                SocketAddress clientAddress = client.socket().getRemoteSocketAddress();

                client.configureBlocking(false);

                logger.info("Accepted connection from " + clientAddress);

                subscriber.subscribe(client, ChannelOperation.READ, handlers.getHandler(client));
            }
        } catch (IOException exception) {
            logger.error("Can't accept new client connection", exception);

            Closeables.closeSilently(client);
        }
    }
}
