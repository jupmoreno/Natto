package ar.edu.itba.pdc.natto.proxy;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.Protocol;

import java.nio.ByteBuffer;

public class ProtocolTask<Message> implements Runnable { // TODO: Callable?
    private final Parser<Message> parser;
    private final Protocol<Message> protocol;

    public ProtocolTask(Parser<Message> parser, Protocol<Message> protocol) {
        this.parser = checkNotNull(parser, "Parser can't be null");
        this.protocol = checkNotNull(protocol, "Protocol can't be null");
    }

    @Override
    public void run() {
        while (parser.hasMessage()) {
            Message request = parser.nextMessage();
            Message response = protocol.process(request);

            if (response != null) { // TODO: Y q pasa si en NULL?
                ByteBuffer buffer = parser.toByteBuffer(response);
                // TODO: Add to write queue in ConnectionHandler
            }
        }
    }
}
