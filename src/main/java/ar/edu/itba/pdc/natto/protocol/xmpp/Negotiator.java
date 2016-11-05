package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;


public interface Negotiator {

    enum verificationState{
        FINISHED, INCOMPLETE, IN_PROCESS, ERR,
    }


    //ver si devuelve int o que
    int handshake(Connection connection);

}
