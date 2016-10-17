package ar.edu.itba.pdc.natto.proxy.handlers;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class DualConnectionHandlerFactory extends ConnectionHandlerFactory {

    @Override
    public ConnectionHandler get(Selector selector, SocketChannel channel) {
        ConnectionHandler handler = null;

        try {
            handler = new DualConnectionHandler(selector, channel);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return handler;
    }
}
