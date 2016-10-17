package ar.edu.itba.pdc.natto.io;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class CachedByteBufferPool implements ByteBufferPool {
    private List<ByteBuffer> buffers = new LinkedList<>();
    private int bufferSize;

    public CachedByteBufferPool(final int bufferSize) {
        this.bufferSize = bufferSize;
        this.buffers.add(ByteBuffer.allocate(bufferSize)); // TODO: Remove?
    }

    @Override
    public ByteBuffer acquire() {
        if (buffers.size() == 0) {
            return ByteBuffer.allocate(bufferSize);
        }

        return buffers.remove(0);
    }

    @Override
    public boolean release(final ByteBuffer buffer) {
        if (buffer.capacity() < bufferSize) {
            return false;
        }

        buffer.clear();
        return buffers.add(buffer);
    }
}
