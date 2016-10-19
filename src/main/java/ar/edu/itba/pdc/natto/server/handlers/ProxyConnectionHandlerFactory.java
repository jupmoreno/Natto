package ar.edu.itba.pdc.natto.server.handlers;

import ar.edu.itba.pdc.natto.server.DispatcherSubscriber;

import java.nio.channels.SocketChannel;

public class ProxyConnectionHandlerFactory implements ConnectionHandlerFactory {
    private final DispatcherSubscriber subscriber;

    public ProxyConnectionHandlerFactory(DispatcherSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public ConnectionHandler getHandler(final SocketChannel channel) {
        return new ProxyConnectionHandler(channel, subscriber);
    }
}
