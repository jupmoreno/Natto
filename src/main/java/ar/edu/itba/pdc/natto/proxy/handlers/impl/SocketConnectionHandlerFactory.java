package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandlerFactory;

import java.nio.channels.SocketChannel;

public class SocketConnectionHandlerFactory implements ConnectionHandlerFactory {
    private final DispatcherSubscriber subscriber;

    public SocketConnectionHandlerFactory(DispatcherSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public SocketConnectionHandler getHandler(final SocketChannel channel) {
        return new SocketConnectionHandler(channel, subscriber);
    }
}
