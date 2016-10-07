package ar.edu.itba.pdc.tpe.buffers;

import java.nio.ByteBuffer;

public interface ByteBufferPool {
    public ByteBuffer adquire();
    public boolean release(ByteBuffer buffer);
}
