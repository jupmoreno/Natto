package ar.edu.itba.pdc.natto.server.handlers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface Connection {
    void requestConnect(final InetSocketAddress serverAddress) throws IOException;

    boolean requestWrite(ByteBuffer buffer) throws IOException; // TODO: Se puede sacar el boolean? Pq la IO Exception?

    void requestRead() throws IOException;
}
