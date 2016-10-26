package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;

public class XmppParserFactory implements ParserFactory<Tag> {

    @Override
    public XmppParser get() {
        return new XmppParser();
    }
}
