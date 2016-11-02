package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

import java.nio.ByteBuffer;

/**
 * Created by user on 26/10/16.
 */
public class XmppProtocolFactory implements ProtocolFactory<ByteBuffer> {
    @Override
    public XmppProtocol get() {
        return new XmppProtocol();
    }
}
