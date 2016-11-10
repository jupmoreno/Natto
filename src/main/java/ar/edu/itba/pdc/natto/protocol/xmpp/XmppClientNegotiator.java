package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.net.NetAddress;
import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

// ASK: No hay q usar encode?
public class XmppClientNegotiator implements ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppClientNegotiator.class);

    private final XmppData data;

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(10000);

    private boolean verified = false;
    private boolean inAuth = false;
    private boolean initialStream = true;

    private StringBuilder auxUser = new StringBuilder();
    private String user;
    private String user64;

    private boolean hasToWrite = false;

    public XmppClientNegotiator(XmppData data) {
        this.data = data;
    }

    @Override
    public void afterConnect(Connection me, Connection other) {
        checkState(false);
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer readBuffer) {
        int ret = handshake(me, readBuffer);

        if (ret == -1) {
            //TODO HACER ALGO MAS JP????
            logger.error("Failed to parse xmpp message");
            me.requestClose();
        } else if (ret == 1) {
            verified = true;
            NetAddress netAddress = data.getUserAddress(user);
            InetSocketAddress socketAddress = new InetSocketAddress(netAddress.getAddress(), netAddress.getPort());

            InetSocketAddress serverAddress = new InetSocketAddress(netAddress.getAddress(), netAddress.getPort());
            XmppServerNegotiator serverNegotiator = new XmppServerNegotiator(data, user64, user);

            System.out.println("conecto con el servidor");
            try {
                me.requestConnect(serverAddress, serverNegotiator);
            } catch (IOException exception) {
                logger.error("Failed to request server connection", exception);
                me.requestClose();
                // TODO: Hacer algo mas? JP
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

    private int handshake(Connection connection, ByteBuffer readBuffer) {
        NegotiationStatus readResult = NegotiationStatus.INCOMPLETE;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                retBuffer.clear();
                reader.getInputFeeder().feedInput(readBuffer);

            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }
        } else {
            // TODO:
            checkState(false);
        }

        while (readResult != NegotiationStatus.FINISHED) {

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }

            switch (readResult) {

                case FINISHED:
                    return 1;

                case IN_PROCESS:
                    connection.requestWrite(retBuffer);
                    retBuffer.clear();
                    break;

                case ERR:
                    if (hasToWrite) {
                        connection.requestWrite(retBuffer);
                        hasToWrite = false;
                    }
                    auxUser.setLength(0);
                    return -1;


                case INCOMPLETE:
                    return 0;
            }

        }

        return 0;
    }

    private NegotiationStatus generateResp() throws XMLStreamException {

        while (reader.hasNext()) {
            switch (reader.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT:
                    NegotiationStatus vs = handleStartDocument();
                    if (vs != null) {
                        return vs;
                    }
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:

                    if (reader.getLocalName().equals("stream") && reader.getPrefix().equals("stream")) {
                        initialStream = false;
                        return handleStreamStream();
                    }
                    handleStartElement();
                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    if (inAuth) {
                        auxUser.append(reader.getText());
                    }
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("auth")) {
                        return getUser();
                    }

                    return NegotiationStatus.IN_PROCESS;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return NegotiationStatus.INCOMPLETE;

                default:
                    break;
            }
        }

        return NegotiationStatus.ERR;
    }

    private NegotiationStatus handleStartDocument() {
        if (reader.getVersion() != null && reader.getEncoding() != null) {
            retBuffer.put("<?xml ".getBytes());
            if (reader.getVersion() != null) {
                retBuffer.put("version='".getBytes()).put(reader.getVersion().getBytes()).put("' ".getBytes());
            }

            if (reader.getEncoding() == null) {
                retBuffer.put("encoding='UTF-8'?>".getBytes());
            } else {
                retBuffer.put("encoding=".getBytes()).put(reader.getVersion().getBytes()).put("?>".getBytes());
            }
            return NegotiationStatus.IN_PROCESS;
        }
        return null;

    }


    private NegotiationStatus handleStartElement() {

        if (reader.getLocalName().equals("auth")) {
            inAuth = true;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (reader.getAttributeLocalName(i).equals("mechanism") && reader.getAttributeValue(i).equals("PLAIN")) {
                    return NegotiationStatus.IN_PROCESS;
                }
            }
            return NegotiationStatus.ERR;
        }

        if (reader.getLocalName().equals("message") || reader.getLocalName().equals("iq") || reader.getLocalName().equals("presence"))
            return handleNotAuthorized();

        return NegotiationStatus.ERR;
    }

    private NegotiationStatus handleStreamStream() {
        retBuffer.put("<stream:stream".getBytes());

        //TODO meter id ver como se hace

        for (int i = 0; i < reader.getAttributeCount(); i++) {

            if (reader.getAttributeLocalName(i).equals("version") && !reader.getAttributeValue(i).equals("\"1.0\"") && !reader.getAttributeValue(i).equals("'1.0'") && !reader.getAttributeValue(i).equals("1.0")) {
                return handleWrongVersion();
            }
            retBuffer.put(" ".getBytes());
            if (!reader.getAttributePrefix(i).isEmpty()) {
                retBuffer.put(reader.getAttributePrefix(i).getBytes()).put(":".getBytes());
            }

            if (reader.getAttributeLocalName(i).equals("to")) {
                retBuffer.put("from=\"".getBytes()).put(reader.getAttributeValue(i).getBytes()).put("\"".getBytes());

            } else if (reader.getAttributeLocalName(i).equals("from")) {
                retBuffer.put("to=\"".getBytes()).put(reader.getAttributeValue(i).getBytes()).put("\"".getBytes());
            } else {
                retBuffer.put(reader.getAttributeLocalName(i).getBytes()).put("=\"".getBytes()).put(reader.getAttributeValue(i).getBytes()).put("\"".getBytes());
            }
        }

        appendNamespaces();

        retBuffer.put("><stream:features><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">".getBytes());
        retBuffer.put("<mechanism>PLAIN</mechanism></mechanisms>".getBytes());

        retBuffer.put("<compression xmlns=\"http://jabber.org/features/compress\">".getBytes());
        retBuffer.put("<method>zlib</method></compression>".getBytes());

        retBuffer.put("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>".getBytes());

        retBuffer.put("</stream:features>".getBytes());

        return NegotiationStatus.IN_PROCESS;
    }

    private void appendNamespaces() {
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            retBuffer.put("xmlns".getBytes());
            if (!reader.getNamespacePrefix(i).isEmpty()) {
                retBuffer.put(":".getBytes()).put(reader.getNamespacePrefix(i).getBytes());
            }
            retBuffer.put("=\"".getBytes()).put(reader.getNamespaceURI(i).getBytes()).put("\" ".getBytes());
        }
    }

    private NegotiationStatus getUser() {
        user64 = auxUser.toString();

        try {
            user = new String(Base64.getDecoder().decode(user64), UTF_8);
            String[] userAndPass = user.split(String.valueOf('\0'), 3);
            user = userAndPass[1];
        } catch (Exception e) {
            return handleInvalidUser();
        }
        return NegotiationStatus.FINISHED;
    }

    /**Error Handlers**/

    /**
     * RFC 4.9.3.25.  unsupported-version
     */
    private NegotiationStatus handleWrongVersion() {
        /* RFC 4.9.1.2. If the error is triggered by the initial stream header, the receiving entity MUST still send the opening <stream> tag*/
        if (initialStream) {
            retBuffer.put("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'".getBytes());
        }
        retBuffer.put("><stream:error><unsupported-version xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return NegotiationStatus.ERR;
    }


    /**
     * RFC 4.9.3.1.  bad-format
     */
    private int handleWrongFormat(Connection connection) {
        /* RFC 4.9.1.2. If the error is triggered by the initial stream header, the receiving entity MUST still send the opening <stream> tag*/
        if (initialStream) {
            connection.requestWrite(ByteBuffer.wrap("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'><stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
            auxUser.setLength(0);
            return -1;
        }
        connection.requestWrite(ByteBuffer.wrap("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
        auxUser.setLength(0);
        return -1;
    }

    /**
     * RFC 4.9.3.12.  not-authorized
     */
    private NegotiationStatus handleNotAuthorized() {
        /* RFC 4.9.1.2. If the error is triggered by the initial stream header, the receiving entity MUST still send the opening <stream> tag*/
        if (initialStream) {
            retBuffer.put("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>".getBytes());
        }
        retBuffer.put("<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return NegotiationStatus.ERR;
    }


    /**
     * RFC 4.9.3.22.  unsupported-encoding
     */
    private NegotiationStatus handleInvalidUser() {
        retBuffer.clear();
        retBuffer.put("<stream:error><unsupported-encoding xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        retBuffer.flip();
        hasToWrite = true;
        return NegotiationStatus.ERR;

    }
}
