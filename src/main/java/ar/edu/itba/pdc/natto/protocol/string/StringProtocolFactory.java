package ar.edu.itba.pdc.natto.protocol.string;

import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

public class StringProtocolFactory implements ProtocolFactory<String> {
    @Override
    public StringProtocol get() {
        return new StringProtocol();
    }
}
