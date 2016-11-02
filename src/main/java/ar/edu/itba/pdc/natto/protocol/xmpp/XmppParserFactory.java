package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;

import java.nio.ByteBuffer;

public class XmppParserFactory implements ParserFactory<ByteBuffer> {

    @Override
    public XmppParser get() {
        return new XmppParser();
    }
}
