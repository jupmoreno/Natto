package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NegotiatorClient implements Negotiator {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(10000);

    private boolean verified = false;
    private boolean inAuth = false;
    private boolean initialStream = true;

    private StringBuilder auxUser = new StringBuilder();
    private String user;
    private String user64;



    private NegotiatorServer neg = null;

    private boolean hasToWrite = false;

    @Override
    public boolean isVerified() {
        if (neg == null || !neg.isVerified())
            return false;

        return verified;
    }

    public int handshake(Connection connection, ByteBuffer readBuffer) {

        if (verified) {
            return 1;
        }

        VerificationState readResult = VerificationState.INCOMPLETE;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                retBuffer.clear();
                reader.getInputFeeder().feedInput(readBuffer);

            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }
        }

        while (readResult != VerificationState.FINISHED) {

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);

            }


            switch (readResult) {

                case FINISHED:
                    connection.requestWrite(ByteBuffer.wrap("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>".getBytes()));
                    verified = true;
//                    NetAddress netAddress = data.getUserAddress(user);
//                    InetSocketAddress socketAddress = new InetSocketAddress(netAddress.getAddress(), netAddress.getPort()); //TODO CAMBIAR
                    try {
                        neg = new NegotiatorServer();
                        neg.setUser64(user64);
                        Connection server = connection.requestConnect(new InetSocketAddress(5222), neg);
                        server.requestWrite(ByteBuffer.wrap(new String("<?xml version=\"1.0\"?>\n" +
                                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">").getBytes()));


                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
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

    private VerificationState generateResp() throws XMLStreamException {

        while (reader.hasNext()) {
            switch (reader.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT:
                    VerificationState vs = handleStartDocument();
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

                    return VerificationState.IN_PROCESS;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return VerificationState.INCOMPLETE;

                default:
                    break;
            }
        }

        return VerificationState.ERR;
    }

    private VerificationState handleStartDocument() {
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
            return VerificationState.IN_PROCESS;
        }
        return null;

    }


    private VerificationState handleStartElement() {

        if (reader.getLocalName().equals("auth")) {
            inAuth = true;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (reader.getAttributeLocalName(i).equals("mechanism") && reader.getAttributeValue(i).equals("PLAIN")) {
                    return VerificationState.IN_PROCESS;
                }
            }
            return VerificationState.ERR;
        }

        if (reader.getLocalName().equals("message") || reader.getLocalName().equals("iq") || reader.getLocalName().equals("presence"))
            return handleNotAuthorized();

        return VerificationState.ERR;
    }

    private VerificationState handleStreamStream() {
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

        retBuffer.put("><stream:features><starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">".getBytes());
        retBuffer.put("<mechanism>PLAIN</mechanism></mechanisms>".getBytes());

        retBuffer.put("<compression xmlns=\"http://jabber.org/features/compress\">".getBytes());
        retBuffer.put("<method>zlib</method></compression>".getBytes());

        retBuffer.put("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>".getBytes());
        retBuffer.put("<register xmlns=\"http://jabber.org/features/iq-register\"/></stream:features>".getBytes());

        return VerificationState.IN_PROCESS;
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

    private VerificationState getUser() {
        user64 = auxUser.toString();

        try {
            user = new String(Base64.getDecoder().decode(user64), UTF_8);
        } catch (Exception e) {
            return handleInvalidUser();
        }

        String[] userAndPass = user64.split(String.valueOf((char) 0));
        user = userAndPass[1];
        return VerificationState.FINISHED;

    }


    /**Error Handlers**/

    /* TODO  If the error is triggered by the initial stream header, the receiving
   entity MUST still send the opening <stream> tag, include the <error/>
   element as a child of the stream element, and send the closing
   </stream> tag (preferably in the same TCP packet).*/



    //TODO: CERRAR CONNECTION ETC

    /**
     * RFC 4.9.3.25.  unsupported-version
     */
    private VerificationState handleWrongVersion() {
        if(initialStream){
            retBuffer.put("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'".getBytes());
        }
        retBuffer.put("><stream:error><unsupported-version xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return VerificationState.ERR;
    }

    //TODO: IDEM CONNECTION

    /**
     * RFC 4.9.3.1.  bad-format
     */
    private int handleWrongFormat(Connection connection) {
        if(initialStream){
            connection.requestWrite(ByteBuffer.wrap("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'><stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
            auxUser.setLength(0);
            return -1;
        }
        connection.requestWrite(ByteBuffer.wrap("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
        auxUser.setLength(0);
        return -1;
    }

    //TODO:cierro connection!
    /**
     * RFC 4.9.3.12.  not-authorized
     */
    private VerificationState handleNotAuthorized() {
        if(initialStream){
            retBuffer.put("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>".getBytes());
        }
        retBuffer.put("<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return VerificationState.ERR;
    }


    //TODO cierro connection!
    /**
     * RFC 4.9.3.22.  unsupported-encoding
     */
    private VerificationState handleInvalidUser() {
        retBuffer.clear();
        retBuffer.put("<stream:error><unsupported-encoding xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        retBuffer.flip();
        hasToWrite = true;
        return VerificationState.ERR;

    }

}
