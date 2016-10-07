package ar.edu.itba.pdc.tpe.proxy.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ConnectionHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private final Selector selector;
    private final SocketChannel client;
    private final SocketChannel server;

    public ConnectionHandler(Selector selector, SocketChannel client, SocketChannel server) {
        this.selector = selector;
        this.client = client;
        this.server = server;
    }

    @Override
    public void handle(int readyOps) throws IOException {
        // TODO: if((readyOps & SelectionKey.OP_CONNECT) == 0) return; ?

        try {
            String clientAddress = client.socket().getRemoteSocketAddress().toString();

            server.finishConnect(); // TODO: if(!finishConnect()) throw new IOException?
            logger.info(clientAddress + " established connection with server on " +
                    server.socket().getRemoteSocketAddress());

            client.register(selector, SelectionKey.OP_READ, new IOHandler(selector, client, server));
            server.register(selector, SelectionKey.OP_READ, new IOHandler(selector, server, client));
            selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando?
        } catch (Exception e) {
            logger.error("Couldn't establish connection with server", e);
            // TODO: Close
        }
    }
}
