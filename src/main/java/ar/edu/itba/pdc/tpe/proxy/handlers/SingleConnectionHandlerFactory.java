package ar.edu.itba.pdc.tpe.proxy.handlers;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SingleConnectionHandlerFactory implements ConnectionHandlerFactory {
    @Override
    public Handler get(Selector selector, SocketChannel channel) {
        return new SingleConnectionHandler(selector, channel);
    }
}
