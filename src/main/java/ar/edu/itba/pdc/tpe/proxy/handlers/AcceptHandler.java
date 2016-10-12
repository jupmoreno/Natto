package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(AcceptHandler.class);

    private final Selector selector;
    private final ServerSocketChannel channel;

    // TODO: Sacar selector (?
    public AcceptHandler(final Selector selector, final ServerSocketChannel channel) {
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

        SocketChannel client = null;

        try {
            // Will immediately return null if there are no pending connections
            client = channel.accept();

            if (client != null) {
                // There are pending connections
                String clientAddress = client.socket().getRemoteSocketAddress().toString();

                client.configureBlocking(false);
                logger.info("Accepted connection from " + clientAddress);
                // TODO: Change key ops
            }
        } catch (IOException exception) {
            logger.error("Error while accepting new client connection", exception);

            if(client != null) {
                client.close();
            }

            throw exception;
        }
    }
}
