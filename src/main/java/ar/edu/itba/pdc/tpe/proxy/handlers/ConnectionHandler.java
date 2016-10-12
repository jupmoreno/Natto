package ar.edu.itba.pdc.tpe.proxy.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ConnectionHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private final Selector selector;
    private final SocketChannel server;

    public ConnectionHandler(final Selector selector, final SocketChannel server) {
        checkNotNull(selector, "Null selector");
        checkArgument(selector.isOpen(), "Invalid selector");
        checkNotNull(server, "Null server channel");
        checkArgument(server.isOpen(), "Invalid server channel");

        this.selector = selector;
        this.server = server;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        checkArgument((readyOps & SelectionKey.OP_CONNECT) != 0);

        String serverAddress;

        try {
            // If this channel is in non-blocking mode then this method will return
            // false if the connection process is not yet complete.
            if(server.finishConnect()) {
                // Connection completed
                serverAddress = server.socket().getRemoteSocketAddress().toString();
                logger.info("Established connection with server on " + serverAddress);
                // TODO: Change key ops
            }
        } catch (IOException exception) {
            logger.error("Couldn't establish connection with server", exception);

            server.close();
            // TODO: Close client
        }
    }
}
