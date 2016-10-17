package ar.edu.itba.pdc.natto.proxy.handlers;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SingleConnectionHandlerFactory extends ConnectionHandlerFactory {
    @Override
    public ConnectionHandler get(Selector selector, SocketChannel channel) {
        return new SingleConnectionHandler(selector, channel);
    }
}
