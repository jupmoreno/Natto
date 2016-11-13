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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class XmppServerNegotiator extends ProtocolHandler implements LinkedProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppServerNegotiator.class);

    private static final int BUFFER_SIZE = 10000;
    private static final ByteBuffer closeBuffer = ByteBuffer.wrap(
            XmppMessages.END_STREAM.getBytes()).asReadOnlyBuffer(); // TODO: Test

    private final AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private final AsyncXMLStreamReader<AsyncByteBufferFeeder> reader =
            inputF.createAsyncForByteBuffer();

    private final ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private boolean authSent = false;
    private boolean inMechanism = false;
    private boolean hasPlain = false;
    private boolean initialStreamReceived = false;
    private boolean featuresReceived = false;
    private boolean inFeatures = false;

    private final XmppData data;
    private final String user64;
    private final String user;
    private final String toServer;

    private String tagClientResponse = null;

    private LinkedProtocolHandler link;

    public XmppServerNegotiator(XmppData data, String user64, String user, String toServer) {
        this.data = checkNotNull(data, "User can't be null");
        this.user64 = checkNotNull(user64, "Base 64 data can't be null");
        this.user = checkNotNull(user, "User can't be null");
        this.toServer = checkNotNull(toServer, "Hostname can't be null");
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
        connection.requestWrite(buffer);
    }

    @Override
    public void finishedWriting() {
        if (retBuffer.hasRemaining()) {
            link.requestWrite(retBuffer);
        } else {
            link.requestClose();
        }
    }

    @Override
    public void requestClose() {
        connection.requestClose();
    }

    @Override
    public void afterConnect() {
        retBuffer.clear();
        retBuffer.put(XmppMessages.VERSION_AND_ENCODING.getBytes());
        retBuffer.put(XmppMessages.INITIAL_STREAM_START.getBytes());
        retBuffer.put("to='".getBytes())
                .put(toServer.getBytes())
                .put("' ".getBytes());
        retBuffer.put("xmlns:xml='http://www.w3.org/XML/1998/namespace'>".getBytes());
        retBuffer.flip();

        connection.requestWrite(retBuffer);
    }

    @Override
    public void afterRead(final ByteBuffer readBuffer) {
        int ret = handshake(readBuffer);

        if (ret == -1) {
            connection.requestWrite(closeBuffer);
            connection.requestClose();

            retBuffer.flip();
            link.requestWrite(retBuffer);
        } else if (ret == 1) {
            authSent = true;

            retBuffer.flip();
            connection.requestWrite(retBuffer);
        } else if (ret == 0) {
            connection.requestRead();
        }
    }

    @Override
    public void afterWrite() {
        if (authSent) {
            if (retBuffer.hasRemaining()) {
                connection.requestWrite(retBuffer);
            } else {
                XmppForwarder handler = new XmppForwarder();
                handler.link(link);
                link.link(handler);

                connection.setHandler(handler);
                connection.requestRead();

                link.requestRead();
            }
        } else {
            if (retBuffer.hasRemaining()) {
                connection.requestWrite(retBuffer);
            } else {
                retBuffer.clear();
                connection.requestRead();
            }
        }
    }

    @Override
    public void beforeClose() {

    }

    public int handshake(ByteBuffer readBuffer) {
        NegotiationStatus readResult;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                // If the state is such that this method should not be called (has not yet
                // consumed existing input data, or has been marked as closed)
                // This should never happen
                handleError(XmppErrors.INTERNAL_SERVER);
                return -1;
            }
        } else {
            // Method called to check whether it is ok to feed more data: parser returns true if
            // it has no more content to parse (and it is ok to feed more); otherwise false
            // (and no data should yet be fed).
            // This should never happen
            handleError(XmppErrors.INTERNAL_SERVER);
            return -1;
        }

        do {
            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                handleError(XmppErrors.BAD_FORMAT);
                return -1;
            }

            switch (readResult) {
                case FINISHED:
                    return 1;

                case IN_PROCESS:
                    break;

                case INCOMPLETE:
                    return 0;

                case ERROR:
                    return -1;

                default:
                    handleError(XmppErrors.INTERNAL_SERVER);
                    return -1;
            }
        } while (readResult != NegotiationStatus.FINISHED);

        return 1;
    }

    private NegotiationStatus generateResp() throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT:
                    return handleStartDocument();

                case AsyncXMLStreamReader.START_ELEMENT:
                    return handleStartElement();

                case AsyncXMLStreamReader.CHARACTERS:
                    if (inMechanism && reader.getText().equalsIgnoreCase("PLAIN")) {
                        hasPlain = true;
                    }
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    return handleEndElement();

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return NegotiationStatus.INCOMPLETE;

                default:
                    break;
            }
        }

        return NegotiationStatus.ERROR;

    }

    private NegotiationStatus handleStartDocument() {
        String version = reader.getVersion();
        String encoding = reader.getEncoding();

        if (version == null && encoding == null) {
            return NegotiationStatus.IN_PROCESS;
        }

        if (encoding != null && !encoding.equals("UTF-8")) {
            handleError(XmppErrors.INTERNAL_SERVER); // TODO: Que error?
            return NegotiationStatus.ERROR;
        }

        return NegotiationStatus.IN_PROCESS;
    }

    private NegotiationStatus handleStartElement() {
        String local = reader.getLocalName();
        String prefix = reader.getPrefix();

        if (!initialStreamReceived) {
            if (local.equals("stream") && prefix != null && prefix.equals("stream")) {
                initialStreamReceived = true;
            } else {
                handleError(XmppErrors.INTERNAL_SERVER); // TODO: Que error?
                return NegotiationStatus.ERROR;
            }
        } else if (!featuresReceived) {
            if (!inFeatures) {
                if (local.equals("features") && prefix != null && prefix.equals("stream")) {
                    inFeatures = true;
                } else {
                    handleError(XmppErrors.INTERNAL_SERVER); // TODO: Que error?
                    return NegotiationStatus.ERROR;
                }
            } else {
                if (local.equals("mechanism")) {
                    inMechanism = true;
                }
            }
        }

        return NegotiationStatus.IN_PROCESS;
    }

    private NegotiationStatus handleEndElement() {
        String local = reader.getLocalName();

        if (local.equals("features")) {
            if (!hasPlain) {
                handleError(XmppErrors.INTERNAL_SERVER); // TODO: Que error?
                return NegotiationStatus.ERROR;
            }

            featuresReceived = true;

            retBuffer.put(XmppMessages.AUTH_START.getBytes())
                    .put(user64.getBytes())
                    .put(XmppMessages.AUTH_STOP.getBytes());

            return NegotiationStatus.FINISHED;
        } else if (local.equals("mechanism")) {
            inMechanism = false;
        }

        return NegotiationStatus.IN_PROCESS;
    }

    private void handleError(XmppErrors error) {
        logger.warn("Server sent messages with errors");

        retBuffer.clear();

        retBuffer.put(error.getBytes());

        if (error.shouldClose()) {
            retBuffer.put(XmppMessages.END_STREAM.getBytes());
        }
    }
}
