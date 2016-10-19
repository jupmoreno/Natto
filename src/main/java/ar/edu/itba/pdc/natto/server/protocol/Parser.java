package ar.edu.itba.pdc.natto.server.protocol;

import java.nio.ByteBuffer;

public interface Parser<T> {
    boolean add(final ByteBuffer buffer);
    boolean hasMessage();
    T nextMessage();
    T fromByteBuffer(final ByteBuffer buffer);
    ByteBuffer toByteBuffer(final T message);
}
