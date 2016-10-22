package ar.edu.itba.pdc.natto.proxy.handlers.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.dispatcher.DispatcherSubscriber;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandlerFactory;

import java.nio.channels.SocketChannel;

// TODO: Se puede sacar <T>?
public class SocketConnectionHandlerFactory<T> implements ConnectionHandlerFactory {
    private final DispatcherSubscriber subscriber;
    private final ParserFactory<T> parserFactory;
    private final ProtocolFactory<T> protocolFactory;

    public SocketConnectionHandlerFactory(DispatcherSubscriber subscriber,
                                          ParserFactory<T> parserFactory,
                                          ProtocolFactory<T> protocolFactory) {
        // TODO: Remove checks?
        this.subscriber = checkNotNull(subscriber, "Register can't be null");
        this.parserFactory = checkNotNull(parserFactory, "Parser factory can't be null");
        this.protocolFactory = checkNotNull(protocolFactory, "Protocol factory can't be null");
    }

    @Override
    public SocketConnectionHandler<T> getHandler(final SocketChannel channel) {
        return new SocketConnectionHandler<>(channel, subscriber, parserFactory, protocolFactory);
    }
}
