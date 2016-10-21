package ar.edu.itba.pdc.natto.proxy.handlers;

import java.nio.channels.SocketChannel;

public interface ConnectionHandlerFactory {
    ConnectionHandler getHandler(final SocketChannel channel);
}
