package ar.edu.itba.pdc.natto.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler implements SelectorHandler {
    private static final Logger logger = LoggerFactory.getLogger(AcceptHandler.class);

    private final Selector selector;
    private final ServerSocketChannel channel;
    private final ConnectionHandlerFactory handlers;

    public AcceptHandler(final Selector selector, final ServerSocketChannel channel,
                         final ConnectionHandlerFactory handlers) {
        checkNotNull(selector, "Selector can't be null");
        checkArgument(selector.isOpen(), "Selector isn't open");

        checkNotNull(channel, "Channel can't be null");
        checkArgument(channel.isOpen(), "Channel isn't open");

        checkNotNull(handlers, "SelectorHandler factory can't be null");

        this.selector = selector;
        this.channel = channel;
        this.handlers = handlers;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        checkArgument((readyOps & SelectionKey.OP_ACCEPT) != 0);

        SocketChannel client = null;

        try {
            // Will immediately return null if there are no pending connections
            client = channel.accept();

            if (client != null) {
                SocketAddress clientAddress = client.socket().getRemoteSocketAddress();

                client.configureBlocking(false);

                logger.info("Accepted connection from " + clientAddress);

                // TODO: Change key ops
                client.register(selector, SelectionKey.OP_READ, handlers.get(selector, client));
            }
        } catch (IOException exception) {
            logger.error("Can't accept new client connection", exception);

            if (client != null) {
                try {
                    client.close();
                } catch (IOException closeException) {
                    logger.error("Can't properly close new client connection with error",
                            closeException);
                }
            }

            throw exception;
        }
    }
}
