package ar.edu.itba.pdc.natto.protocol.xmpp;

/**
 * Created by natinavas on 11/5/16.
 */
public class XmppNegotiatorFactory implements NegotiatorFactory {


    @Override
    public Negotiator get() {
        return new NegotiatorClient();
    }
}
