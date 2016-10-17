package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Protocol;

public class StringProtocol implements Protocol<String> {
    @Override
    public String process(final String message) {
        System.out.println(message);
        return message;
    }
}
