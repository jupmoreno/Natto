package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;

public class XmppParserFactory implements ParserFactory<String> {

    @Override
    public XmppParser get() {
        return new XmppParser();
    }
}
