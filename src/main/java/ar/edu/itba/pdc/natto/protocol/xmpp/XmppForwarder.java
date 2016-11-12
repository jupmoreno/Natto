package ar.edu.itba.pdc.natto.protocol.xmpp;

import static com.google.common.base.Preconditions.checkState;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;


public class XmppForwarder implements ProtocolHandler {
    private static final int BUFFER_SIZE = 1024;

    private ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    @Override
    public void afterConnect(Connection me, Connection other) {
        checkState(false);
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer buffer) {
        retBuffer.clear();
        retBuffer.put(buffer);
        retBuffer.flip();

        other.requestWrite(buffer);
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        if (retBuffer.hasRemaining()) {
            other.requestWrite(retBuffer);
        } else {
            me.requestRead();
        }
    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }
}
