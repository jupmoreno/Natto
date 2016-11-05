package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;

/**
 * Created by user on 05/11/16.
 */
public class NttpParserFactory implements ParserFactory<StringBuilder> {
    @Override
    public Parser<StringBuilder> get() {
        return new NttpParser();
    }
}
