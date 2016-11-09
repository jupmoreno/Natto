package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

public class XmppProtocolHandler implements ProtocolHandler {
    @Override
    public void afterConnect(Connection me, Connection other) {

    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer buffer) {

    }

    @Override
    public void afterWrite(Connection me, Connection other) {

    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }
}
