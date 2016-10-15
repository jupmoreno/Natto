package ar.edu.itba.pdc.tpe.io;

import java.nio.ByteBuffer;

public interface ByteBufferPool {
    ByteBuffer acquire();

    boolean release(final ByteBuffer buffer);
}
