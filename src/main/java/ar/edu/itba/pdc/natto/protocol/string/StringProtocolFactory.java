package ar.edu.itba.pdc.natto.protocol.string;

public class StringProtocolFactory implements ProtocolFactory<String> {
    @Override
    public StringProtocol get() {
        return new StringProtocol();
    }
}
