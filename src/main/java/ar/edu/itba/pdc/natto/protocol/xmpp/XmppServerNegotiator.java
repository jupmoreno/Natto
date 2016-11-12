package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("Duplicates") // TODO: Remove
public class XmppServerNegotiator implements ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppServerNegotiator.class);

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(100000);

    private boolean inMech = false;
    private boolean hasPlain = false;
    private boolean hasToWrite = false;

    private boolean haveToSendAuth = false;
    private boolean sentAuth = false;

    private boolean verified = false;
    private String user64;
    private String username;
    private final String toServer;

    private String tagClientResponse = null;

    private final XmppData data;

    public XmppServerNegotiator(XmppData data, String user64, String username) {
        this.user64 = user64;
        this.username = username;
        this.data = data;
        this.toServer = "localhost"; // TODO: Recibir en parametros
    }


    @Override
    public void afterConnect(Connection me, Connection other) {
        retBuffer.clear();
        retBuffer.put(XmppMessages.VERSION_AND_ENCODING.getBytes());
        retBuffer.put(XmppMessages.INITIAL_STREAM_START.getBytes());
        retBuffer.put("to='".getBytes()).put(toServer.getBytes()).put("' ".getBytes());
        retBuffer.put("xmlns:xml='http://www.w3.org/XML/1998/namespace'>".getBytes());
        retBuffer.flip();

        me.requestWrite(retBuffer);

//        me.requestWrite(ByteBuffer.wrap(("<?xml version=\"1.0\"?>" +
//                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
//                "version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" " +
//                "xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">").getBytes()));

    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer readBuffer) {
        if (sentAuth) {
            // TODO
            readBuffer.put(readBuffer);
            retBuffer.flip();
            other.requestWrite(retBuffer);
            return;
        }

        int ret = handshake(me, other, readBuffer);

        if (ret == -1) {
            // TODO Error
            me.requestClose();
            other.requestWrite(ByteBuffer.wrap(("<stream:error>" +
                    "<internal-server-error xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
                    "</stream:error>" +
                    "</stream:stream>").getBytes()));
            other.requestClose();


            // TODO: Mandarle algo al cliente y cerrarlo
        } else if (ret == 1) {
            me.setHandler(new XmppParser(data));
            me.requestRead();

            // TODO: JP!

            other.requestWrite(retBuffer);
            other.setHandler(new XmppParser(data));
            other.requestRead();

        } else if (ret == 0) {
            me.requestRead();
        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        if (retBuffer.hasRemaining()) {
            me.requestWrite(retBuffer);
        } else {
            me.requestRead();
        }
    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }

    public int handshake(Connection me, Connection other, ByteBuffer readBuffer) {

        NegotiationStatus readResult = NegotiationStatus.INCOMPLETE;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                // if the state is such that this method should not be called (has not yet
                // consumed existing input data, or has been marked as closed)
                // TODO: This should never happen
                checkState(false);

                handleError(XmppErrors.INTERNAL_SERVER);
                return -1;
            }
        } else {
            // Method called to check whether it is ok to feed more data: parser returns true if
            // it has no more content to parse (and it is ok to feed more); otherwise false
            // (and no data should yet be fed).
            // TODO: This should never happen
            checkState(false);
            handleError(XmppErrors.INTERNAL_SERVER);
            return -1;
        }

        while (readResult != NegotiationStatus.FINISHED) {

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                return handleWrongFormat(me);
            }


            switch (readResult) {
                case FINISHED:
                    verified = true;
                    return 1;

                case IN_PROCESS:

                    if (hasToWrite) {
                        me.requestWrite(retBuffer);
                        hasToWrite = false;

                    } else if (haveToSendAuth) {
                        me.requestWrite(retBuffer);
                        haveToSendAuth = false;
                        sentAuth = true;

                    } else if (sentAuth) {
                        other.requestWrite(retBuffer);
                    }

                    break;

                case INCOMPLETE:
                    return 0;

                case ERROR:
                    if (hasToWrite) {
                        me.requestWrite(retBuffer);
                    }
                    me.requestClose();
                    return -1;
            }

        }

        return 0;
    }


    private NegotiationStatus generateResp() throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT: //TODO remove?
                    break;


                case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:   //TODO remove?
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    return handleStartElement();

                case AsyncXMLStreamReader.CHARACTERS:
                    if (inMech && reader.getText().equals("PLAIN")) {
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


    private NegotiationStatus handleEndElement() {

        if (reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")) {
            String ret = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">" + user64 + "</auth>";
            retBuffer.clear(); //TODO ?
            retBuffer = ByteBuffer.wrap(ret.getBytes());
            haveToSendAuth = true;
            return NegotiationStatus.IN_PROCESS;
        }
        if (reader.getName().equals("mechanisms")) {
            inMech = false;
        }

        /*In these two cases this is the last tag of the negotiator*/
        if (sentAuth && tagClientResponse != null && tagClientResponse.equals(reader.getName().toString())) {
            if (reader.getLocalName().equals("success")) {
                return NegotiationStatus.FINISHED;
            } else {
                return NegotiationStatus.ERROR;
            }
        }

        if (sentAuth && tagClientResponse == null) {
            if (reader.getLocalName().equals("success")) {
                return NegotiationStatus.FINISHED;
            } else {
                return NegotiationStatus.ERROR;
            }
        }

        return NegotiationStatus.IN_PROCESS;

    }

    private NegotiationStatus handleStartElement() {

        if (sentAuth && tagClientResponse == null) {
            tagClientResponse = reader.getName().toString();
        }

        if (reader.getLocalName().equals("mechanism")) {
            inMech = true;
        }

        if (reader.getLocalName().equals("message") || reader.getLocalName().equals("iq") || reader.getLocalName().equals("presence")) {
            return handleNotAuthorized();
        }


        return NegotiationStatus.IN_PROCESS;

    }

    /**Error Handlers**/


    /**
     * RFC 4.9.3.1.  bad-format
     */
    private int handleWrongFormat(Connection connection) {
        connection.requestWrite(ByteBuffer.wrap("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
        return -1;

    }

    /**
     * RFC 4.9.3.12.  not-authorized
     */
    private NegotiationStatus handleNotAuthorized() {
        retBuffer.clear();
        retBuffer = ByteBuffer.wrap("<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return NegotiationStatus.ERROR;
    }
}
