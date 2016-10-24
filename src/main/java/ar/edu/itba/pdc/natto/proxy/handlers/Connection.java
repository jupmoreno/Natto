package ar.edu.itba.pdc.natto.proxy.handlers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO: Se pueden sacar los throws IOException?
public interface Connection {
    void requestConnect(final InetSocketAddress serverAddress) throws IOException;

    void requestWrite(ByteBuffer buffer) throws IOException;

    void requestRead() throws IOException;

    void requestClose() throws IOException;

    // TODO: Hace falta?
    void forceClose() throws IOException;
}
