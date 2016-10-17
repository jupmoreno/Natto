package ar.edu.itba.pdc.natto.io;

import java.nio.ByteBuffer;

public interface ByteBufferPool {
    ByteBuffer acquire();

    boolean release(final ByteBuffer buffer);
}
