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

public final class ProxyAcceptHandler extends ServerAcceptHandler {
    private final Logger logger = LoggerFactory.getLogger(ProxyAcceptHandler.class);

    private final InetSocketAddress serverAddress;

    protected SocketChannel server;

    public ProxyAcceptHandler(final Selector selector, final ServerSocketChannel channel,
                              final InetSocketAddress serverAddress) {
        super(selector, channel);

        checkNotNull(serverAddress, "Null server address");
        checkArgument(!serverAddress.isUnresolved(), "Invalid server address");
        this.serverAddress = serverAddress;
    }

    @Override
    public void handle(final int readyOps) throws IOException {
        super.handle(readyOps);

        String clientAddress = client.getRemoteAddress().toString();
        server = null;

        try {
            server = SocketChannel.open();

            server.configureBlocking(false);
            server.connect(serverAddress);

            logger.info(clientAddress + " requested server connection");

            // TODO: Change key ops
            checkNotNull(client.keyFor(selector), "No key registered").cancel();
            server.register(selector, SelectionKey.OP_CONNECT, new ProxyConnectionHandler(selector,
                    client, server));
        } catch (IOException exception) {
            logger.error(clientAddress + " couldn't establish connection with server", exception);

            close();

            throw exception;
        }
    }

    @Override
    protected void close() throws IOException {
        super.close();

        if (server != null) {
            server.close();
        }
    }
}
