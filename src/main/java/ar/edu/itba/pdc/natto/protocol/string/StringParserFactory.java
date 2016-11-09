package ar.edu.itba.pdc.natto.protocol.string;

public class StringParserFactory implements ParserFactory<String> {
    @Override
    public StringParser get() {
        return new StringParser();
    }
}
