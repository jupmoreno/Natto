package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StringParser implements Parser<String> {
    private Queue<ByteBuffer> buffers;
    private Queue<String> messages;

    public StringParser() {
        buffers = new ConcurrentLinkedQueue<>();
        messages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean add(final ByteBuffer buffer) {
        return buffers.offer(buffer);
    }

    @Override
    public boolean hasMessage() {
        if (!messages.isEmpty()) {
            return true;
        }

        return processBuffers();
    }

    @Override
    public String nextMessage() {
        return messages.poll();
    }

    @Override
    public ByteBuffer toByteBuffer(final String message) {
        return ByteBuffer.wrap(message.getBytes());
    }

    private boolean processBuffers() {
        String request;
        ByteBuffer buffer = buffers.peek();

        if (buffer == null) {
            return false;
        }

        request = new String(buffer.array(), buffer.position(), buffer.limit(),
                Charset.forName("UTF-8"));

        if (!messages.offer(request)) {
            return false;
        }

        buffers.remove();
        return true;
    }
}
