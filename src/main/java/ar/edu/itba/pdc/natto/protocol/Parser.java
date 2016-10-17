package ar.edu.itba.pdc.natto.protocol;

import java.nio.ByteBuffer;

public interface Parser<Message> {
    void add(final ByteBuffer buffer);
    boolean hasMessage();
    Message nextMessage();
    ByteBuffer toByteBuffer(final Message message);
}
