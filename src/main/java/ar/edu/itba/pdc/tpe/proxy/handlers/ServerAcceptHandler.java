package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerAcceptHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(ServerAcceptHandler.class);

    protected final Selector selector;
    protected final ServerSocketChannel channel;

    protected SocketChannel client;

    // TODO: Sacar selector (?
    public ServerAcceptHandler(final Selector selector, final ServerSocketChannel channel) {
        checkNotNull(selector, "Null selector");
        checkArgument(selector.isOpen(), "Invalid selector");
        checkNotNull(channel, "Null channel");
        checkArgument(channel.isOpen(), "Invalid channel");

        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        checkArgument((readyOps & SelectionKey.OP_ACCEPT) != 0);

        client = null;

        try {
            // Will immediately return null if there are no pending connections
            client = channel.accept();

            if (client != null) {
                // There are pending connections
                String clientAddress = client.socket().getRemoteSocketAddress().toString();
                client.configureBlocking(false);

                logger.info("Accepted connection from " + clientAddress);

                // TODO: Change key ops
                client.register(selector, SelectionKey.OP_READ, new IOHandler(selector,
                        client, client));
            }
        } catch (IOException exception) {
            logger.error("Error while accepting new client connection", exception);

            close();

            throw exception;
        }
    }

    protected void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
