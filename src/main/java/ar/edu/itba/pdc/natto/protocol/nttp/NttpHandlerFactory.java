package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;

public class NttpHandlerFactory implements ProtocolHandlerFactory {

    private final XmppData xmppData;

    public NttpHandlerFactory(XmppData xmppData) {
        this.xmppData = xmppData;
    }

    @Override
    public ProtocolHandler get() {
        return new NttpHandler(xmppData);
    }
}

