package ar.edu.itba.pdc.natto.protocol.xmpp;

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
