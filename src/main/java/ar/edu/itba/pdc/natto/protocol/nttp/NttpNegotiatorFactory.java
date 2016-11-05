package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.protocol.NegotiatorFactory;

/**
 * Created by user on 05/11/16.
 */
public class NttpNegotiatorFactory implements NegotiatorFactory{

    @Override
    public Negotiator get() {
        return new NttpNegotiator();
    }
}
