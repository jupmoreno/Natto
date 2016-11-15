package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.LinkedProtocolHandler;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

public class XmppParser extends ProtocolHandler implements LinkedProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppParser.class);

    private final static int BUFFER_MAX_SIZE = 10000;

    private final AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private final AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();

    private final ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_MAX_SIZE);
    private final XmppData xmppData;
    private final String user;
    private final String toServer;

    private LinkedProtocolHandler link;

    private boolean inMessage = false;
    private boolean inBody = false;
    private boolean ignoreMessage = false;

    public XmppParser(XmppData data, String user, String toServer) {
        this.xmppData = data;
        this.user = user;
        this.toServer = toServer;
    }

    @Override
    public void link(LinkedProtocolHandler link) {
        this.link = link;
    }

    @Override
    public void requestRead() {
        connection.requestRead();
    }

    @Override
    public void requestWrite(ByteBuffer buffer) {
        if (ignoreMessage) {
            return;
        }

        int before = buffer.remaining();
        connection.requestWrite(buffer);
        xmppData.moreBytesTransferred(before - buffer.remaining());
    }

    @Override
    public void finishedWriting() {
        if (retBuffer.hasRemaining()) {
            link.requestWrite(retBuffer);
        } else {
            retBuffer.clear();
            if (parser.getInputFeeder().needMoreInput()) {
                connection.requestRead();
            } else {
                callParse();
            }
        }
    }

    private void callParse() {
        int ret = parse();

        if (ret == -1) {
            checkState(false);
            // TODO: JPM ERROR
        } else if (ret == 1) {
            retBuffer.flip();

            if (ignoreMessage && retBuffer.hasRemaining()) {
                int before = retBuffer.remaining();
                connection.requestWrite(retBuffer);
                xmppData.moreBytesTransferred(before - retBuffer.remaining());
            } else {
                link.requestWrite(retBuffer);
            }
        } else {
            connection.requestRead();
        }
    }

    @Override
    public void requestClose() {
        connection.requestClose();
    }

    @Override
    public void afterConnect() {
        throw new IllegalStateException("Not a connectable handler");
    }

    @Override
    public void afterRead(ByteBuffer buffer) {
        if (parser.getInputFeeder().needMoreInput()) {
            try {
                parser.getInputFeeder().feedInput(buffer);
            } catch (XMLStreamException e) {
                // if the state is such that this method should not be called (has not yet
                // consumed existing input data, or has been marked as closed)
                // TODO: This should never happen JPM
                checkState(false);
                // Al cliente XmppErrors.INTERNAL_SERVER
                // Al servidor </stream:stream>
                return;
            }
        } else {
            // Method called to check whether it is ok to feed more data: parser returns true if
            // it has no more content to parse (and it is ok to feed more); otherwise false
            // (and no data should yet be fed).
            // TODO: This should never happen JPM
            checkState(false);
            // Al cliente XmppErrors.INTERNAL_SERVER
            // Al servidor </stream:stream>
            return;
        }

        callParse();
    }

    @Override
    public void afterWrite() {
        if (ignoreMessage) {
            if (retBuffer.hasRemaining()) {
                int before = retBuffer.remaining();
                connection.requestWrite(retBuffer);
                xmppData.moreBytesTransferred(before - retBuffer.remaining());
            } else {
                retBuffer.clear();

                if (parser.getInputFeeder().needMoreInput()) {
                    connection.requestRead();
                } else {
                    callParse();
                }
            }
        }

        link.finishedWriting();
    }

    @Override
    public void beforeClose() {
        // TODO JPM
    }

    private int parse() {
        try {
            while (parser.hasNext()) {
                switch (parser.next()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        handleStartDocument();
                        break;

                    case AsyncXMLStreamReader.START_ELEMENT:
                        handleStartElement();
                        return 1;

                    case AsyncXMLStreamReader.CHARACTERS:
                        handleCharacters();
                        return 1;

                    case AsyncXMLStreamReader.END_ELEMENT:
                        handleEndElement();
                        return 1;

                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return 0;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException exception) {
            logger.error("Can't parse message ", exception);
            return -1;
        }

        return 1;
    }


    private boolean handleStartDocument() {

        retBuffer.put(XmppMessages.VERSION_AND_ENCODING.getBytes());

        return true;
    }

    private void handleStartElement() {
        String local = parser.getLocalName();
        String prefix = parser.getPrefix();
        String toUser = null;

        if (ignoreMessage) {
            return;
        }

        if (local.equals("message")) {
            if (xmppData.isUserSilenced(user)) {
                ignoreMessage = true;
            } else {
                inMessage = true;
            }
        } else if (local.equals("body") && inMessage) {
            inBody = true;
        }

        retBuffer.put("<".getBytes());
        if (prefix != null && !prefix.isEmpty()) {
            retBuffer.put(prefix.getBytes())
                    .put(":".getBytes());
        }

        retBuffer.put(local.getBytes());
        retBuffer.put(" ".getBytes());

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (ignoreMessage && parser.getAttributeLocalName(i).equals("from")) {
                retBuffer.put("to='".getBytes())
                        .put(parser.getAttributeValue(i).getBytes())
                        .put("' from='".getBytes())
                        .put(toServer.getBytes())
                        .put("' ".getBytes());
            } else if (ignoreMessage && parser.getAttributeLocalName(i).equals("to")) {
                // Ignore
            } else {
                if (inMessage && parser.getAttributeLocalName(i).equals("to")) {
                    toUser = parser.getAttributeValue(i);
                }

                if (!parser.getAttributePrefix(i).isEmpty()) {
                    retBuffer.put(parser.getAttributePrefix(i).getBytes())
                            .put(":".getBytes());
                }

                retBuffer.put(parser.getAttributeLocalName(i).getBytes())
                        .put("='".getBytes())
                        .put(parser.getAttributeValue(i).getBytes())
                        .put("' ".getBytes());
            }
        }

        for (int i = 0; i < parser.getNamespaceCount(); i++) {
            retBuffer.put("xmlns".getBytes());
            if (!parser.getNamespacePrefix(i).isEmpty()) {
                retBuffer.put(":".getBytes())
                        .put(parser.getNamespacePrefix(i).getBytes());
            }

            retBuffer.put("='".getBytes())
                    .put(parser.getNamespaceURI(i).getBytes())
                    .put("' ".getBytes());
        }

        retBuffer.put(">".getBytes());

        if (ignoreMessage) {
            retBuffer.put(("<error by='" + toServer + "' type='modify'><policy-violati" +
                    "on xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error></message>").getBytes());
        }

        if (toUser != null) {
            String[] username = toUser.split("@", 2);

            if (xmppData.isUserSilenced(username[0])) {
                retBuffer.clear();
                ignoreMessage = true;
            }
        }
    }

    public void handleCharacters() {
        if (ignoreMessage) {
            return;
        }

        if (inBody) {
            for (char c : parser.getText().toCharArray()) {
                transform(c);
            }
        } else {
            retBuffer.put(parser.getText().getBytes());
        }
    }

    private void transform(char c) {
        boolean changed = false;

        if (xmppData.isTransformEnabled()) {
            switch (c) {
                case 'a':
                    changed = true;
                    retBuffer.put("4".getBytes());
                    break;

                case 'e':
                    changed = true;
                    retBuffer.put("3".getBytes());
                    break;

                case 'i':
                    changed = true;
                    retBuffer.put("1".getBytes());
                    break;

                case 'o':
                    changed = true;
                    retBuffer.put("0".getBytes());
                    break;

                case 'c':
                    changed = true;
                    retBuffer.put("&lt;".getBytes());
                    break;
            }
        }

        if (!changed) {
            switch (c) {
                case '<':
                    retBuffer.put("&lt;".getBytes());
                    break;

                case '>':
                    retBuffer.put("&gt;".getBytes());
                    break;

                case '&':
                    retBuffer.put("&amp;".getBytes());
                    break;

                case '\'':
                    retBuffer.put("&apos;".getBytes());
                    break;

                case '\"':
                    retBuffer.put("&quot;".getBytes());
                    break;

                default:
                    retBuffer.put(String.valueOf(c).getBytes());
                    break;
            }
        }
    }

    private void handleEndElement() {
        String local = parser.getLocalName();
        String prefix = parser.getPrefix();

        if (ignoreMessage) {
            if (local.equals("message")) {
                ignoreMessage = false;
            }

            return;
        }

        retBuffer.put("</".getBytes());

        if (prefix != null && !prefix.isEmpty()) {
            retBuffer.put(parser.getPrefix().getBytes())
                    .put(":".getBytes());
        }

        retBuffer.put(local.getBytes())
                .put(">".getBytes());

        if (local.equals("body") && inMessage) {
            inBody = false;
        } else if (local.equals("message")) {
            inMessage = false;
        }
    }
}
