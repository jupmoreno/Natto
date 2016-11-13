package ar.edu.itba.pdc.natto.protocol;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

public abstract class ProtocolHandler {
    protected Connection connection;

    public void setConnection(Connection connection) {
        // TODO: Validar algo?
        this.connection = connection;
    }

    public abstract void afterConnect();

    public abstract void afterRead(final ByteBuffer buffer);

    public abstract void afterWrite();

    public abstract void beforeClose();
}
