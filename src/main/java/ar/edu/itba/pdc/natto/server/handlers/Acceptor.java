package ar.edu.itba.pdc.natto.server.handlers;

import ar.edu.itba.pdc.natto.server.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.server.io.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Acceptor implements AcceptHandler {
    private static final Logger logger = LoggerFactory.getLogger(Acceptor.class);

    private final DispatcherSubscriber subscriber;
    private final ServerSocketChannel channel;

    public Acceptor(final ServerSocketChannel channel, final DispatcherSubscriber subscriber) {
        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");

        this.channel = channel;
        this.subscriber = checkNotNull(subscriber, "Register can't be null");
    }

    @Override
    public void handle_accept() throws IOException {
        SocketChannel client = null;

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

                subscriber.subscribe(client, SelectionKey.OP_READ, ); // TODO:
            }

        } catch (IOException exception) {
            logger.error("Can't accept new client connection", exception);

            Channels.closeSilently(client);
        }
    }
}
