package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

public class NttpHandler implements ProtocolHandler {
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
    public void afterConnect(Connection me, Connection other) {
        checkState(false);
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer buffer) {

        while (!shouldClose && buffer.hasRemaining()) {
            StringBuilder request = parser.parse(buffer);

            if (request != null) {
                StringBuilder response = protocol.process(request);
                System.out.println("RESPONSE: " + response); // TODO: Remove
                if (response != null) {
                    me.requestWrite(toByteBuffer(response));
                    if (isQuit(response)) {
                        shouldClose = true;
                    }
                }
            } else {
                me.requestRead();
            }

        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        if (buffer.hasRemaining()) {
            me.requestWrite(buffer);
        } else {
            if (shouldClose) {
                me.requestClose();
            } else {
                me.requestRead();
            }
        }
    }

    @Override
    public void beforeClose(Connection me, Connection other) {
        // TODO:
    }

    private boolean isQuit(StringBuilder command) {
        String[] commandStrs = command.toString().split(" ");

        if (commandStrs.length == 4) {
            return commandStrs[0].equals(".") && commandStrs[1].equals("10");
        }

        if (commandStrs.length > 4) {
            return commandStrs[1].equals(".") && commandStrs[2].equals("10");
        }

        return false;
    }

    private ByteBuffer toByteBuffer(StringBuilder message) {
        buffer.clear();
        // TODO: Ver
        buffer.put(message.toString().getBytes());
        buffer.flip();

        return buffer;
    }
}
