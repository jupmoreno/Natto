package ar.edu.itba.pdc.natto.proxy.handlers;

import ar.edu.itba.pdc.natto.protocol.Negotiator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Connection {
    Connection requestConnect(final InetSocketAddress serverAddress, final Negotiator negotiator) throws IOException;

    void requestWrite(ByteBuffer buffer);

    void requestRead();

    void requestClose();

    // TODO: Hace falta?
//    void forceClose();
}
