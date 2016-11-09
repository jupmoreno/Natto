package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;

public class XmppHandlerFactory implements ProtocolHandlerFactory {

    private final XmppData data;

    public XmppHandlerFactory(XmppData data) {
        this.data = data;
    }

    @Override
    public ProtocolHandler get() {
        return new XmppClientNegotiator(data);
    }
}
