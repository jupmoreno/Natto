package ar.edu.itba.pdc.natto.proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.natto.dispatcher.ChannelOperation;
import ar.edu.itba.pdc.natto.dispatcher.Dispatcher;
import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;
import ar.edu.itba.pdc.natto.proxy.handlers.AcceptHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandlerFactory;
import ar.edu.itba.pdc.natto.proxy.handlers.impl.Acceptor;
import ar.edu.itba.pdc.natto.proxy.handlers.impl.ProxyConnectionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;

public class MultiProtocolServer implements Server {
    private static final Logger logger = LoggerFactory.getLogger(MultiProtocolServer.class);

    private boolean running = false;

    private final Map<ServerSocketChannel, Integer> sockets;

    private final Dispatcher dispatcher;

    private MultiProtocolServer(Map<ServerSocketChannel, Integer> sockets,
                                Dispatcher dispatcher) {
        this.sockets = sockets;
        this.dispatcher = dispatcher;
    }

    public static class Builder {
        private final Map<ServerSocketChannel, Integer> sockets;

        private final Dispatcher dispatcher;
        private final DispatcherSubscriber subscriber;

        public Builder(Dispatcher dispatcher) {
            sockets = new HashMap<>();
            this.dispatcher = dispatcher;
            this.subscriber = dispatcher.getSubscriber();
        }

        public Builder addProtocol(int port, ProtocolHandlerFactory factory) throws IOException {
            checkArgument(port > 0 && port <= 65535, "Invalid port number");
            checkNotNull(factory, "Protocol handler factory can't be null");

            ServerSocketChannel channel = ServerSocketChannel.open();
            // Adjusts channel to non blocking mode
            channel.configureBlocking(false);

            ConnectionHandlerFactory connectionHandlers = new ProxyConnectionHandlerFactory(
                    subscriber, factory);
            AcceptHandler acceptHandler = new Acceptor(channel, subscriber, connectionHandlers);

            subscriber.subscribe(channel, ChannelOperation.ACCEPT, acceptHandler);

            sockets.put(channel, port);

            return this;
        }

        public MultiProtocolServer build() {
            return new MultiProtocolServer(sockets, dispatcher);
        }
    }

    @Override
    public void start() throws IOException {
        checkState(!running);

        try {
            for (ServerSocketChannel channel : sockets.keySet()) {
                InetSocketAddress address = new InetSocketAddress(sockets.get(channel));

                // Configures the socket to listen for connections
                channel.socket().bind(address);

                logger.info("Proxy Server now listening on: " + address);
            }
        } catch (IOException exception) {
            logger.error("Couldn't start Proxy Server", exception);
            // TODO: Cerrar los anterioes a los q fallaron (?
            throw exception;
        }

        running = true;

        try {
            while (running) {
                dispatcher.handle_events();
            }
        } catch (IOException exception) {
            logger.error("Dispatcher force closed", exception);
        } finally {
            //stop(); //TODO DESCOMENTAR???
        }
    }

    @Override
    public void stop() {
        checkState(running);

        running = false;
        // TODO:
        // TODO: Cerrar channels
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
