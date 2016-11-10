package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandlerFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;

public class NttpHandlerFactory implements ProtocolHandlerFactory {

    private final NttpData nttpData;
    private final XmppData xmppData;

    public NttpHandlerFactory(NttpData nttpData, XmppData xmppData) {
        this.nttpData = nttpData;
        this.xmppData = xmppData;
    }

    @Override
    public NttpHandler get() {
        return new NttpHandler(nttpData, xmppData);
    }
}

