package ar.edu.itba.pdc.tpe.proxy.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ConnectionHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private final Selector selector;
    private final SocketChannel client;
    private final SocketChannel server;

    public ConnectionHandler(final Selector selector, final SocketChannel client,
                             final SocketChannel server) {
        checkNotNull(selector, "Null selector");
        checkArgument(selector.isOpen(), "Invalid selector");
        checkNotNull(client, "Null client channel");
        checkArgument(client.isOpen(), "Invalid client channel");
        checkNotNull(server, "Null server channel");
        checkArgument(server.isOpen(), "Invalid server channel");

        this.selector = selector;
        this.client = client;
        this.server = server;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        checkArgument((readyOps & SelectionKey.OP_CONNECT) != 0);

        try {
            String clientAddress = client.socket().getRemoteSocketAddress().toString();
            String serverAddress;

            server.finishConnect(); // TODO: if(!finishConnect()) throw new IOException?
            serverAddress = server.socket().getRemoteSocketAddress().toString();
            logger.info(clientAddress + " established connection with server on " + serverAddress);

            client.register(selector, SelectionKey.OP_READ, new IOHandler(selector, client, server));
            server.register(selector, SelectionKey.OP_READ, new IOHandler(selector, server, client));
            selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando?
        } catch (Exception e) {
            logger.error("Couldn't establish connection with server", e);
            // TODO: Close
        }
    }
}
