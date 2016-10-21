package ar.edu.itba.pdc.natto.protocol;

public interface Protocol<T> {
    T process(T message);
}
