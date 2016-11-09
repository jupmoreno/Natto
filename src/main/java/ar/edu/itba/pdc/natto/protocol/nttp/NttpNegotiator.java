package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;


public class NttpNegotiator implements Negotiator {
    @Override
    public boolean isVerified() {
        return true;
    }

    @Override
    public int handshake(Connection connection, ByteBuffer readBuffer) {
        return 0;
    }
}
