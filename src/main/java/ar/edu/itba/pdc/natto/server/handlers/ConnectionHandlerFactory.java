package ar.edu.itba.pdc.natto.server.handlers;

import java.nio.channels.SocketChannel;

public interface ConnectionHandlerFactory {
    ConnectionHandler getHandler(final SocketChannel channel);
}
