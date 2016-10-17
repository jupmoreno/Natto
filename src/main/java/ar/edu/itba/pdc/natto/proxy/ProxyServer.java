package ar.edu.itba.pdc.natto.proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.StringParserFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.StringProtocolFactory;
import ar.edu.itba.pdc.natto.proxy.handlers.*;

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
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final InetSocketAddress serverAddress;
    private final int xmppPort;
    private final int pspPort;

    private final ExecutorService executors;

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

        executors = Executors.newCachedThreadPool(); // TODO: Aca? O en start()?
    }

    public void start() throws IOException {
        checkState(!isRunning());

        try (
                Selector selector = Selector.open();
                ServerSocketChannel xmppChannel = ServerSocketChannel.open();
//                ServerSocketChannel pspChannel = ServerSocketChannel.open();
        ) {
            final ServerSocket xmppSocket = xmppChannel.socket();
//            final ServerSocket pspSocket = pspChannel.socket();

            // Adjusts channel to non blocking mode
            xmppChannel.configureBlocking(false);
//            pspChannel.configureBlocking(false);

            ParserFactory xmppParserFactory = new StringParserFactory();
            ProtocolFactory xmppProtocolFactory = new StringProtocolFactory();
            ConnectionHandlerFactory xmppHandlerFactory = new DualConnectionHandlerFactory(
                    xmppParserFactory, xmppProtocolFactory);

            // Register channel with the selector
            xmppChannel.register(selector, SelectionKey.OP_ACCEPT,
                    new AcceptHandler(selector, xmppChannel, new DualConnectionHandlerFactory()));
//            pspChannel.register(selector, SelectionKey.OP_ACCEPT,
//                    new AcceptHandler(selector, pspChannel, new SingleConnectionHandlerFactory()));

            // Configures the socket to listen for connections
            xmppSocket.bind(new InetSocketAddress(xmppPort));
//            pspSocket.bind(new InetSocketAddress(pspPort));

            logger.info("Proxy Server now listening");
            running = true;
            serve(selector);
        } catch (IOException exception) {
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
            stop();
        }
    }

    private void dispatch(final SelectionKey key) {
        SelectorHandler handler = checkNotNull((SelectorHandler) key.attachment());

        try {
            handler.handle(key.readyOps());
        } catch (IOException exception) {
            logger.error("Handling error", exception);
            // TODO:
        }
    }
}
