package ar.edu.itba.pdc.tpe.proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.tpe.proxy.handlers.AcceptHandler;
import ar.edu.itba.pdc.tpe.proxy.handlers.Handler;
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
    private final int port;

    private final ExecutorService executors;

    private boolean running = false;

    public ProxyServer(final InetSocketAddress serverAddress, final int port) {
        checkNotNull(serverAddress, "Null server address");
        checkArgument(!serverAddress.isUnresolved(), "Invalid server address");
        checkArgument(port > 0, "Invalid port number");

        this.serverAddress = serverAddress;
        this.port = port;

        executors = Executors.newCachedThreadPool(); // TODO: Aca? O en start()?
    }

    public void start() throws IOException {
        checkState(!isRunning());

        try (
                Selector selector = Selector.open();
                ServerSocketChannel channel = ServerSocketChannel.open();
        ) {
            ServerSocket socket = channel.socket();

            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler(selector,
                    channel, serverAddress));
            socket.bind(new InetSocketAddress(port));

            running = true;
            handleConnections(selector);
        } catch (IOException exception) {
            logger.error("Couldn't start ProxyServer", exception);
            // TODO:
            throw exception;
        }
    }

    private void handleConnections(final Selector selector) {
        try {
            while (isRunning()) {
                if (selector.select() != 0) { // TODO: Timeout
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove(); // http://stackoverflow.com/q/7132057/3349531

                        dispatch(key);
                    }
                }
            }
        } catch (IOException exception) {
            running = false;
            logger.error("Server closed", exception);
            // TODO:
        }
    }

    private void dispatch(final SelectionKey key) {
        if (!key.isValid()) {
            return;
        }

        Handler handler = (Handler) key.attachment();

        try {
            handler.handle(key.readyOps()); // TODO: Sacar throws IOException?
        } catch (IOException exception) {
            logger.error("Handling error", exception);
            // TODO:
        }
    }

    public boolean isRunning() {
        return running;
    }
}
