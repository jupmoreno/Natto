package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.protocol.NegotiatorFactory;

/**
 * Created by natinavas on 11/5/16.
 */
public class XmppNegotiatorFactory implements NegotiatorFactory {


    @Override
    public Negotiator get() {
        return new NegotiatorClient();
    }
}
