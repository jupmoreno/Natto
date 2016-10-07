package ar.edu.itba.pdc.tpe.proxy.handlers;

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
    private final InetSocketAddress serverAddress;

    public AcceptHandler(Selector selector, ServerSocketChannel channel, InetSocketAddress
            serverAddress) {
        this.selector = selector;
        this.channel = channel;
        this.serverAddress = serverAddress;
    }

    @Override
    public void handle(int readyOps) throws IOException {
        // TODO: if((readyOps & SelectionKey.OP_ACCEPT) == 0) return; ?

        SocketChannel client;
        SocketChannel server;

        try {
            client = channel.accept();

            if (client != null) {
                String clientAddress = client.socket().getRemoteSocketAddress().toString();

                client.configureBlocking(false);
                logger.info("Accepted connection from " + clientAddress);

                server = SocketChannel.open(); // ASK: Aca?
                server.configureBlocking(false);
                server.connect(serverAddress);
                logger.info(clientAddress + " requested server connection");

                server.register(selector, SelectionKey.OP_CONNECT, new ConnectionHandler(selector, client, server));
                selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando?
            } // TODO: else?
        } catch (IOException e) {
            logger.error(serverAddress + " couldn't establish connection with client", e);
            // TODO: Close
        }
    }
}

