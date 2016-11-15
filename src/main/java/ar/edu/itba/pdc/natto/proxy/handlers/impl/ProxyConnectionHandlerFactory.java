package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandlerFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;

import java.nio.channels.SocketChannel;

public class ProxyConnectionHandlerFactory implements ConnectionHandlerFactory {
    private final DispatcherSubscriber subscriber;
    private final ProtocolHandlerFactory factory;

    public ProxyConnectionHandlerFactory(DispatcherSubscriber subscriber,
                                         ProtocolHandlerFactory factory) {
        this.subscriber = checkNotNull(subscriber, "Register can't be null");
        this.factory = checkNotNull(factory, "Handler factory can't be null");
    }

    @Override
    public ProxyConnectionHandler getHandler(final SocketChannel channel) {
        return new ProxyConnectionHandler(channel, subscriber, factory.get());
    }
}
