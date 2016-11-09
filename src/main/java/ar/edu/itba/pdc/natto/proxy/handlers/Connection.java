package ar.edu.itba.pdc.natto.proxy.handlers;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Connection {
    void setHandler(ProtocolHandler handler);

    void requestConnect(final InetSocketAddress address,
                              final ProtocolHandler serverProtocol) throws IOException;

    void requestWrite(ByteBuffer buffer);

    void requestRead();

    boolean isAlive();

    void requestClose();
}
