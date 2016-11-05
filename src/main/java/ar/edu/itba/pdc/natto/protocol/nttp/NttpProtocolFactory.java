package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

/**
 * Created by user on 05/11/16.
 */
public class NttpProtocolFactory implements ProtocolFactory<StringBuilder> {
    @Override
    public Protocol<StringBuilder> get() {
        return new NttpProtocol();
    }
}
