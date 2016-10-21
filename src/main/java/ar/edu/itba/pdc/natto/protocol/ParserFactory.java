package ar.edu.itba.pdc.natto.protocol;

public interface ParserFactory<T> {
    Parser<T> get();
}
