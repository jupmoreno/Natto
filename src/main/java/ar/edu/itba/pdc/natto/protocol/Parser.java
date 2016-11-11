package ar.edu.itba.pdc.natto.protocol;

import java.nio.ByteBuffer;

public interface Parser<T> {
    T parse(final ByteBuffer buffer);
}