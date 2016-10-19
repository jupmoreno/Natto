package ar.edu.itba.pdc.natto.server.protocol;

public interface ParserFactory<Message> {
    Parser<Message> get();
}
