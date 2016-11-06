package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;

import java.nio.ByteBuffer;

public class XmppParserFactory implements ParserFactory<ByteBuffer> {

    private XmppData data;

    public XmppParserFactory(XmppData data){
        this.data = data;
    }

    @Override
    public XmppParser get() {
        return new XmppParser(data);
    }
}
