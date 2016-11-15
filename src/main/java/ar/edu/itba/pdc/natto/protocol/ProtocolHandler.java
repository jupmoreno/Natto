package ar.edu.itba.pdc.natto.protocol;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ProtocolHandler {
    protected Connection connection;

    public void setConnection(Connection connection) {
        this.connection = checkNotNull(connection);
    }

    public abstract void afterConnect();

    public abstract void afterRead(final ByteBuffer buffer);

    public abstract void afterWrite();

    public abstract void beforeClose();
}
