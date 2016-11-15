package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

public class NttpHandler extends ProtocolHandler {
    private static final int MAX_SIZE = 5000;
    private ByteBuffer buffer = ByteBuffer.allocate(MAX_SIZE);

    private NttpProtocol protocol;
    private NttpParser parser;
    private boolean shouldClose = false;

    public NttpHandler(NttpData nttpData, XmppData xmppData) {
        protocol = new NttpProtocol(nttpData, xmppData);
        parser = new NttpParser();
    }

    @Override
    public void afterConnect() {
        throw new IllegalStateException("Not a connectable handler");
    }

    @Override
    public void afterRead(ByteBuffer buffer) {

        while (!shouldClose && buffer.hasRemaining()) {
            StringBuilder request = parser.parse(buffer);

            if (request != null) {
                StringBuilder response = protocol.process(request);
                if (response != null) {
                    connection.requestWrite(toByteBuffer(response));
                    if (isQuit(response)) {
                        shouldClose = true;
                    }
                }
            } else {
                connection.requestRead();
            }

        }
    }

    @Override
    public void afterWrite() {
        if (buffer.hasRemaining()) {
            connection.requestWrite(buffer);
        } else {
            if (shouldClose) {
                connection.requestClose();
            } else {
                connection.requestRead();
            }
        }
    }

    @Override
    public void beforeClose() {

    }

    private boolean isQuit(StringBuilder command) {
        String[] commandStrs = command.toString().split(" ");

        if (commandStrs.length == 4) {
            return commandStrs[0].equals(".") && commandStrs[1].equals("11");
        }

        if (commandStrs.length > 4) {
            return commandStrs[1].equals(".") && commandStrs[2].equals(NttpCode.BYE_BYE.getCode());
        }

        return false;
    }

    private ByteBuffer toByteBuffer(StringBuilder message) {
        buffer.clear();
        buffer.put(message.toString().getBytes());
        buffer.flip();
        return buffer;
    }
}
