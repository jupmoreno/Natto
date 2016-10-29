package ar.edu.itba.pdc.natto.proxy.handlers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Connection {
    void requestConnect(final InetSocketAddress serverAddress) throws IOException;

    void requestWrite(ByteBuffer buffer);

    void requestRead();

    void requestClose();

    // TODO: Hace falta?
//    void forceClose();
}
