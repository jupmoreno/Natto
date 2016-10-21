package ar.edu.itba.pdc.natto.protocol;

import java.nio.ByteBuffer;

public interface Parser<T> {
    T fromByteBuffer(final ByteBuffer buffer);

    ByteBuffer toByteBuffer(final T message);
}
