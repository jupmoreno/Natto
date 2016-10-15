package ar.edu.itba.pdc.tpe.proxy.handlers;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface ConnectionHandlerFactory {
    Handler get(Selector selector, SocketChannel channel);
}
