package ar.edu.itba.pdc.natto.protocol;

public interface ParserFactory<Message> {
    Parser<Message> get();
}
