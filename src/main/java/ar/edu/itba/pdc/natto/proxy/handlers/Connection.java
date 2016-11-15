package ar.edu.itba.pdc.natto.proxy.handlers;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Connection {
    void setHandler(ProtocolHandler handler);

    boolean requestConnect(final InetSocketAddress address, final ProtocolHandler serverProtocol);

    boolean requestWrite(ByteBuffer buffer);

    boolean requestRead();

    boolean isAlive();

    void requestClose();
}
