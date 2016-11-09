package ar.edu.itba.pdc.natto.protocol;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

public interface ProtocolHandler {
    void afterConnect(final Connection me, final Connection other);

    void afterRead(final Connection me, final Connection other, final ByteBuffer buffer);

    void afterWrite(final Connection me, final Connection other);

    void beforeClose(final Connection me, final Connection other);
}
