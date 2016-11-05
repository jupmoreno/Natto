package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;

/**
 * Created by user on 05/11/16.
 */
public class NttpProtocolFactory implements ProtocolFactory<StringBuilder> {

    private final XmppData xmppData;

    public NttpProtocolFactory(XmppData xmppData) {
        this.xmppData = xmppData;
    }


    @Override
    public Protocol<StringBuilder> get() {
        return new NttpProtocol(xmppData);
    }
}
