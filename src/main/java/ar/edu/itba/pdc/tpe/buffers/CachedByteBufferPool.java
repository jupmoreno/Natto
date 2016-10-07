package ar.edu.itba.pdc.tpe.buffers;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class CachedByteBufferPool implements ByteBufferPool {
    private List<ByteBuffer> buffers = new LinkedList<>();
    private int bufferSize;

    public CachedByteBufferPool(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public ByteBuffer adquire() {
        if(buffers.size() == 0) {
            return ByteBuffer.allocate(bufferSize);
        }

        return buffers.remove(0);
    }

    @Override
    public boolean release(ByteBuffer buffer) {
        if(buffer.capacity() < bufferSize) {
            return false;
        }

        buffer.clear();
        return buffers.add(buffer);
    }
}
