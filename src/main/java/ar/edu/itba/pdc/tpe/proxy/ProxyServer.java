package ar.edu.itba.pdc.tpe.proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.tpe.proxy.handlers.Handler;
import ar.edu.itba.pdc.tpe.proxy.handlers.ProxyAcceptHandler;
import ar.edu.itba.pdc.tpe.proxy.handlers.ServerAcceptHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
    private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final InetSocketAddress serverAddress;
    private final int xmppPort;
    private final int pspPort;

    private final ExecutorService executors;

    private boolean running = false;

    public ProxyServer(final InetSocketAddress serverAddress, final int xmppPort,
                       final int pspPort) {
        checkNotNull(serverAddress, "Server address is NULL");
        checkArgument(!serverAddress.isUnresolved(), "Unresolved server address");
        checkArgument(xmppPort > 0, "Invalid XMPP port number");
        checkArgument(pspPort > 0, "Invalid PSP port number");

        this.serverAddress = serverAddress;
        this.xmppPort = xmppPort;
        this.pspPort = pspPort;

        executors = Executors.newCachedThreadPool(); // TODO: Aca? O en start()?
    }

    public void start() throws IOException {
        checkState(!isRunning());

        try (
                Selector selector = Selector.open();
                ServerSocketChannel xmppChannel = ServerSocketChannel.open();
                ServerSocketChannel pspChannel = ServerSocketChannel.open();
        ) {
            final ServerSocket xmppSocket = xmppChannel.socket();
            final ServerSocket pspSocket = pspChannel.socket();

            // Adjusts channel to non blocking mode
            xmppChannel.configureBlocking(false);
            pspChannel.configureBlocking(false);

            // Register channel with the selector
            xmppChannel.register(selector, SelectionKey.OP_ACCEPT, new ProxyAcceptHandler(selector,
                    xmppChannel, serverAddress));
            pspChannel.register(selector, SelectionKey.OP_ACCEPT, new ServerAcceptHandler(selector,
                    pspChannel));

            // Configures the socket to listen for connections
            xmppSocket.bind(new InetSocketAddress(xmppPort));
            pspSocket.bind(new InetSocketAddress(pspPort));

            running = true;
            serve(selector);
        } catch (IOException exception) {
            logger.error("Couldn't start ProxyServer", exception);
            throw exception;
        }
    }

    public void stop() {
        // TODO:
        throw new UnsupportedOperationException();
    }

    private void serve(final Selector selector) {
        try {
            while (isRunning()) {
                if (selector.select() != 0) { // TODO: Timeout (?
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove(); // http://stackoverflow.com/q/7132057/3349531

                        if (key.isValid()) {
                            dispatch(key);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            logger.error("Server closed", exception);
            // TODO:
        } finally {
            running = false;
        }
    }

    private void dispatch(final SelectionKey key) {
        Handler handler = (Handler) key.attachment();

        checkNotNull(handler, "Handler shouldn't be null");

        try {
            handler.handle(key.readyOps());
        } catch (IOException exception) {
            logger.error("Handling error", exception);
            // TODO:
        }
    }

    public boolean isRunning() {
        return running;
    }
}
