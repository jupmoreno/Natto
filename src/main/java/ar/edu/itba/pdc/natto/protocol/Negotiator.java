package ar.edu.itba.pdc.natto.protocol;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;


public interface Negotiator {

    enum VerificationState {
        FINISHED, INCOMPLETE, IN_PROCESS, ERR,
    }


    boolean isVerified();

    //ver si devuelve int o que
    int handshake(Connection connection, ByteBuffer readBuffer);

}
