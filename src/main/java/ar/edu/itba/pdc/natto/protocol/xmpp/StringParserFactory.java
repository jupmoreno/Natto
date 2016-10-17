package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;

public class StringParserFactory implements ParserFactory<String> {
    @Override
    public Parser<String> get() {
        return new StringParser();
    }
}
