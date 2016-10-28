package ar.edu.itba.pdc.natto.protocol.string;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringParser implements Parser<String> {
    @Override
    public String fromByteBuffer(final ByteBuffer buffer) {
        return new String(buffer.array(), buffer.position(), buffer.limit(),
                StandardCharsets.UTF_8);
    }

    @Override
    public ByteBuffer toByteBuffer(final String message) {
        return ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
    }
}
