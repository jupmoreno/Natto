package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;
import ar.edu.itba.pdc.natto.protocol.nttp.NttpHandler;

public class XmppHandlerFactory implements ProtocolHandlerFactory {

    private final XmppData data;

    public XmppHandlerFactory(XmppData data) {
        this.data = data;
    }

    @Override
    public XmppClientNegotiator get() {
        return new XmppClientNegotiator(data);
    }
}
