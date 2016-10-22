package ar.edu.itba.pdc.natto.protocol.string;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;

public class StringParserFactory implements ParserFactory<String> {
    @Override
    public StringParser get() {
        return new StringParser();
    }
}
