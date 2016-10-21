package ar.edu.itba.pdc.natto.protocol;

public interface ProtocolFactory<T> {
    Protocol<T> get();
}
