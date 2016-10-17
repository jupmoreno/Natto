package ar.edu.itba.pdc.natto.proxy.handlers;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class ConnectionHandlerFactory {
    protected final ParserFactory parserFactory;
    protected final ProtocolFactory protocolFactory;

    public ConnectionHandlerFactory(ParserFactory parserFactory, ProtocolFactory protocolFactory) {
        this.parserFactory = parserFactory;
        this.protocolFactory = protocolFactory;
    }

    abstract ConnectionHandler get(Selector selector, SocketChannel channel);
}
