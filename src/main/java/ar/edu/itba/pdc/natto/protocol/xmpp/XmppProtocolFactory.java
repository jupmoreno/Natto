package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;

/**
 * Created by user on 26/10/16.
 */
public class XmppProtocolFactory implements ProtocolFactory<Tag> {
    @Override
    public XmppProtocol get() {
        return new XmppProtocol();
    }
}
