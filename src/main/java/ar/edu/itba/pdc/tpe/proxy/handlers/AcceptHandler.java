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
    private final InetSocketAddress serverAddress;

    public AcceptHandler(final Selector selector, final ServerSocketChannel channel,
                         final InetSocketAddress serverAddress) {
        checkNotNull(selector, "Null selector");
        checkArgument(selector.isOpen(), "Invalid selector");
        checkNotNull(channel, "Null channel");
        checkArgument(channel.isOpen(), "Invalid channel");
        checkNotNull(serverAddress, "Null server address");
        checkArgument(!serverAddress.isUnresolved(), "Invalid server address");

        this.selector = selector;
        this.channel = channel;
        this.serverAddress = serverAddress;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        checkArgument((readyOps & SelectionKey.OP_ACCEPT) != 0);

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

                server.register(selector, SelectionKey.OP_CONNECT, new ConnectionHandler(selector,
                        client, server));
                selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando?
            } // TODO: else?
        } catch (IOException exception) {
            logger.error(serverAddress + " couldn't establish connection with client", exception);
            // TODO: Close
        }
    }
}

