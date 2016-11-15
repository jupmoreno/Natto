package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.LinkedProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;

import java.nio.ByteBuffer;


public class XmppForwarder extends ProtocolHandler implements LinkedProtocolHandler {
    private static final int BUFFER_SIZE = 1024;

    private final ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private LinkedProtocolHandler link;

    private XmppData data;

    public XmppForwarder(XmppData data) {
        this.data = data;
        data.newAccepted();
    }


    @Override
    public void link(LinkedProtocolHandler link) {
        this.link = link;
    }

    @Override
    public void requestRead() {
        connection.requestRead();
    }

    @Override
    public void requestWrite(ByteBuffer buffer) {
        connection.requestWrite(buffer);
    }

    @Override
    public void finishedWriting() {
        if (retBuffer.hasRemaining()) {
            link.requestWrite(retBuffer);
        } else {
            retBuffer.clear();
            connection.requestRead();
        }
    }

    @Override
    public void requestClose() {
        connection.requestClose();
    }

    @Override
    public void afterConnect() {
        throw new IllegalStateException("Not a connectable handler");
    }

    @Override
    public void afterRead(ByteBuffer buffer) {
        retBuffer.put(buffer);
        retBuffer.flip();
        link.requestWrite(retBuffer);
    }

    @Override
    public void afterWrite() {
        link.finishedWriting();
    }

    @Override
    public void beforeClose() {
        // TODO
    }
}
