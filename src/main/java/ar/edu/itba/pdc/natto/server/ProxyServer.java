package ar.edu.itba.pdc.natto.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

public class ProxyServer {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final InetSocketAddress serverAddress;
    private final int xmppPort;
    private final int pspPort;

    private boolean running = false;

    public ProxyServer(final InetSocketAddress serverAddress, final int xmppPort,
                       final int pspPort) {
        checkNotNull(serverAddress, "Server address can't be null");
        checkArgument(!serverAddress.isUnresolved(), "Unresolved server address");
        checkArgument(xmppPort > 0 && xmppPort <= 65535, "Invalid XMPP port number");
        checkArgument(pspPort > 0 && pspPort <= 65535, "Invalid PSP port number");

        this.serverAddress = serverAddress;
        this.xmppPort = xmppPort;
        this.pspPort = pspPort;
    }

    public void start() throws IOException {
        checkState(!isRunning());

        try (
                Dispatcher dispatcher = new ConcreteDispatcher();
                ServerSocketChannel xmppChannel = ServerSocketChannel.open()
        ) {
            final ServerSocket xmppSocket = xmppChannel.socket();

            // Adjusts channel to non blocking mode
            xmppChannel.configureBlocking(false);

            dispatcher.getSubscriber().subscribe(xmppChannel, SelectionKey.OP_ACCEPT, ); // TODO

            // Configures the socket to listen for connections
            xmppSocket.bind(new InetSocketAddress(xmppPort));
            logger.info("Proxy Server now listening");

            running = true;

            loop(dispatcher);

        } catch (IOException exception) {
            // TODO:
            logger.error("Couldn't start Proxy Server", exception);
            throw exception;
        }
    }

    public void stop() {
        checkState(isRunning());

        // TODO:
        running = false;
        throw new UnsupportedOperationException();
    }

    public boolean isRunning() {
        return running;
    }

    private void loop(Dispatcher dispatcher) {
        try {
            while (isRunning()) {
                dispatcher.handle_events();
            }
        } catch (IOException exception) {
            logger.error("Dispatcher force closed", exception);
        } finally {
            stop();
        }
    }
}
