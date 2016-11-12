package ar.edu.itba.pdc.natto.protocol.xmpp;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import ar.edu.itba.pdc.natto.net.NetAddress;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;
import javax.xml.stream.XMLStreamException;

public class XmppClientNegotiator implements ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppClientNegotiator.class);

    private static final int BUFFER_SIZE = 10000;

    private static long id = 0;

    private final AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private final AsyncXMLStreamReader<AsyncByteBufferFeeder> reader =
            inputF.createAsyncForByteBuffer();

    private final ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private boolean inAuth = false;
    private boolean invalidAuth = false;
    private boolean initialStreamSent = false;
    private boolean initialStreamReceived = false;
    private boolean hasToWrite = false;

    private final StringBuilder userBuilder = new StringBuilder();
    private String user;
    private String user64;
    private String toServer;

    private final XmppData data;

    public XmppClientNegotiator(XmppData data) {
        this.data = data;
    }

    @Override
    public void afterConnect(Connection me, Connection other) {
        throw new IllegalStateException("Not a connectable handler");
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer readBuffer) {
        int ret = handshake(me, readBuffer);

        if (hasToWrite) {
            hasToWrite = false;
            retBuffer.flip();
            me.requestWrite(retBuffer);
        }

        if (ret == -1) {
            me.requestClose();
        } else if (ret == 1) {
            logger.info("User " + user + " connected");

            NetAddress netAddress = data.getUserAddress(user);
            InetSocketAddress serverAddress = new InetSocketAddress(netAddress.getAddress(),
                    netAddress.getPort());
            XmppServerNegotiator serverNegotiator = new XmppServerNegotiator(data, user64, user,
                    toServer);

            try {
                me.requestConnect(serverAddress, serverNegotiator);
            } catch (IOException | IllegalArgumentException exception) {
                logger.error("Failed to request server connection", exception);

                handleError(XmppErrors.REMOTE_CONNECTION_FAILED); // TODO: Este error?
                retBuffer.flip();
                me.requestWrite(retBuffer);
                me.requestClose();
            }
        }

        if (ret != 0) {
            try {
                reader.closeCompletely(); // TODO: O usar close() ????
            } catch (XMLStreamException exception) {
                logger.error("Failed to correctly close parser", exception);
            }
        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        if (retBuffer.hasRemaining()) {
            me.requestWrite(retBuffer);
        } else {
            retBuffer.clear();
            me.requestRead();
        }
    }

    @Override
    public void beforeClose(Connection me, Connection other) {
        // TODO:
    }

    private int handshake(Connection connection, ByteBuffer readBuffer) {
        NegotiationStatus readResult;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException exception) {
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
            } catch (XMLStreamException exception) {
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
                    if (!invalidAuth && inAuth) {
                        userBuilder.append(reader.getText());
                    } else {
                        // Ignore
                    }
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("auth")) {
                        if (!invalidAuth) {
                            return getUser();
                        }

                        invalidAuth = false;
                        inAuth = false;
                    } else {
                        checkState(false); // TODO: For testing...
                    }

                    return NegotiationStatus.IN_PROCESS;

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

        if (version != null && !version.equals("1.0")) {
            // Optional
            handleError(XmppErrors.INVALID_XML);
            return NegotiationStatus.ERROR;
        }

        if (encoding != null && !encoding.equals("UTF-8")) {
            handleError(XmppErrors.UNSUPPORTED_ENCODING);
            return NegotiationStatus.ERROR;
        }

        return NegotiationStatus.IN_PROCESS;
    }

    private NegotiationStatus handleStartElement() {
        String local = reader.getLocalName();

        if (!initialStreamReceived) {
            if (local.equals("stream")) {
                return handleStream();
            } else {
                // TODO: Ignorar todo hasta q llegue un stream no?
                return NegotiationStatus.IN_PROCESS;
            }
        } else {
            if (local.equals("stream")) {
                // closing the existing stream for this entity
                // because a new stream has been initiated that conflicts with the
                // existing stream
                handleError(XmppErrors.CONFLICT);
                return NegotiationStatus.ERROR;
            }

            if (local.equals("auth")) {
                return handleAuth();
            }

            if (local.equals("message") || local.equals("iq") || local.equals("presence")) {
                handleError(XmppErrors.NOT_AUTHORIZED);
                return NegotiationStatus.ERROR;
            }

            // TODO: Ver si hay mas casos validos
            handleError(XmppErrors.UNSOPPORTED_STANZA_TYPE);
            return NegotiationStatus.ERROR;
        }
    }

    private NegotiationStatus handleStream() {
        String prefix = reader.getPrefix();

        // The entity has sent a namespace prefix that is unsupported, or has
        // sent no namespace prefix on an element that needs such a prefix
        if (prefix == null || !prefix.equals("stream")) {
            handleError(XmppErrors.BAD_NAMESPACE_PREFIX);
            return NegotiationStatus.ERROR;
        }

        // The root <stream/> element ("stream header") MUST be qualified by the namespace
        // 'http://etherx.jabber.org/streams'
        if (!reader.getNamespaceURI("stream").equals("http://etherx.jabber.org/streams")) {
            handleError(XmppErrors.INVALID_NAMESPACE);
            return NegotiationStatus.ERROR;
        }

        return handleStreamStream();
    }

    private NegotiationStatus handleStreamStream() {
        retBuffer.put(XmppMessages.VERSION_AND_ENCODING.getBytes());
        retBuffer.put(XmppMessages.INITIAL_STREAM_START.getBytes());

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (reader.getAttributeLocalName(i).equals("version")) {
                if (!reader.getAttributeValue(i).equals("1.0")) {
                    handleError(XmppErrors.UNSUPPORTED_VERSION);
                    return NegotiationStatus.ERROR;
                }
            } else if (reader.getAttributeLocalName(i).equals("to")) {
                toServer = reader.getAttributeValue(i);
                retBuffer.put("from='".getBytes())
                        .put(toServer.getBytes())
                        .put("' ".getBytes());
            } else if (reader.getAttributeLocalName(i).equals("from")) {
                retBuffer.put("to='".getBytes())
                        .put(reader.getAttributeValue(i).getBytes())
                        .put("' ".getBytes());
            }
        }

        if (toServer == null) {
            handleError(XmppErrors.HOST_UNKNOWN); // TODO: Este error?
            return NegotiationStatus.ERROR;
        }

        retBuffer.put("id='".getBytes())
                .put(String.valueOf(id).getBytes())
                .put("'>".getBytes());

        retBuffer.put(XmppMessages.STREAM_FEATURES.getBytes());

        initialStreamReceived = true;
        initialStreamSent = true;
        hasToWrite = true;

        return NegotiationStatus.IN_PROCESS;
    }

    private NegotiationStatus handleAuth() {
        inAuth = true;

        if (!reader.getPrefix().equals("")) {
            handleError(XmppErrors.BAD_NAMESPACE_PREFIX);
            return NegotiationStatus.ERROR;
        }

        if (!reader.getNamespaceURI().equals("urn:ietf:params:xml:ns:xmpp-sasl")) {
            handleError(XmppErrors.INVALID_NAMESPACE);
            return NegotiationStatus.ERROR;
        }

        // TODO: Validar q solo haya 1 atributo (mechanism)?
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (reader.getAttributeLocalName(i).equals("mechanism")) {
                String mech = reader.getAttributeValue(i);

                if (mech.equals("PLAIN")) {
                    return NegotiationStatus.IN_PROCESS;
                } else {
                    invalidAuth = true;
                    handleError(XmppErrors.INVALID_MECHANISM);
                    return NegotiationStatus.IN_PROCESS;
                }
            }
        }

        return NegotiationStatus.ERROR;
    }

    private NegotiationStatus getUser() {
        boolean error = false;
        byte[] base64;

        try {
            base64 = Base64.getDecoder().decode(userBuilder.toString());
            user64 = new String(base64, UTF_8);
            String[] userAndPass = user64.split(String.valueOf('\0'), 3);

            user = userAndPass[1]; // TODO: Validar?
        } catch (Exception exception) {
            handleError(XmppErrors.INCORRECT_ENCODING);
            return NegotiationStatus.ERROR;
        }

        return NegotiationStatus.FINISHED;
    }

    private void handleError(XmppErrors error) {
        logger.warn("Client sent messages with errors");

        retBuffer.clear();

        // If the error is triggered by the initial stream header, the receiving entity MUST still
        // send the opening '<stream>' tag.
        if (!initialStreamSent) {
            retBuffer.put(XmppMessages.INITIAL_STREAM.getBytes());
            initialStreamSent = true;
        }

        retBuffer.put(error.getBytes());

        if (error.shouldClose()) {
            retBuffer.put(XmppMessages.END_STREAM.getBytes());
        }

        userBuilder.setLength(0);
        hasToWrite = true;
    }
}