package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;


public class NttpHandler implements ProtocolHandler {

    private NttpProtocol protocol;
    private NttpParser parser;

    public NttpHandler(XmppData xmppData){
        protocol = new NttpProtocol(xmppData);
        parser = new NttpParser();
    }

    @Override
    public void afterConnect(Connection me, Connection other) {
        checkState(false);
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer buffer) {

        while (buffer.hasRemaining()) {

            StringBuilder request = parser.fromByteBuffer(buffer);

            if (request != null) {
                StringBuilder response = protocol.process(request);
                System.out.println("RESPONSE: " + response); // TODO: Remove
                if (response != null) {
                    me.requestWrite(parser.toByteBuffer(response));
                    if(isQuit(response)){
                        me.requestClose(); //TODO: esta bien?
                    }
                }
            }

        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        me.requestRead();
    }

    @Override
    public void beforeClose(Connection me, Connection other) {
        // TODO:
    }

    private boolean isQuit(StringBuilder command){
        String[] commandStrs = command.toString().split(" ");

        if(commandStrs.length == 4){
            return commandStrs[0].equals(".") && commandStrs[1].equals("10");
        }

        if(commandStrs.length > 4){
            return commandStrs[1].equals(".") && commandStrs[2].equals("10");
        }

        return false;
    }
}
