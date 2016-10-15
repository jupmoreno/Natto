package ar.edu.itba.pdc.tpe.proxy.handlers;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class DualConnectionHandlerFactory implements ConnectionHandlerFactory {
    @Override
    public Handler get(Selector selector, SocketChannel channel) {
        Handler handler = null;

        try {
            handler = new DualConnectionHandler(selector, channel);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return handler;
    }
}
