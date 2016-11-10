package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import ar.edu.itba.pdc.natto.proxy.handlers.impl.Acceptor;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

public class XmppServerNegotiator implements ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmppServerNegotiator.class);

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(100000);

    private boolean inMech = false;
    private boolean hasPlain = false;
    private boolean hasToWrite = false;
    private boolean sentAuth = false;

    private boolean verified = false;
    private String user64;
    private String username;

    private ByteBuffer clientResponse = ByteBuffer.allocate(100000);
    private String tagClientResponse = null;

    private final XmppData data;

    public XmppServerNegotiator(XmppData data, String user64, String username) {
        this.user64 = user64;
        this.username = username;
        this.data = data;
    }


    @Override
    public void afterConnect(Connection me, Connection other) {
        me.requestWrite(ByteBuffer.wrap(("<?xml version=\"1.0\"?>" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
                "version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" " +
                "xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">").getBytes()));

    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer readBuffer) {
        int ret = handshake(me, readBuffer);

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

            //TODOOOOOO!!!!!!!!!!!!!
            // TODO: crear nuevos handlers
            me.setHandler(new XmppParser(data));
            me.requestRead();
            // TODO: JP!
            other.setHandler(new XmppParser(data));
            other.requestRead();
//            other.requestWrite(ByteBuffer.wrap("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>".getBytes()));
        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        me.requestRead();

    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }

    public int handshake(Connection connection, ByteBuffer readBuffer) {

        NegotiationStatus readResult = NegotiationStatus.INCOMPLETE;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }
        }

        while (readResult != NegotiationStatus.FINISHED) {

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }


            switch (readResult) {
                case FINISHED:
                    verified = true;
                    retBuffer.clear();
                    return 1;

                case IN_PROCESS:
                    if (hasToWrite) {
                        connection.requestWrite(retBuffer);
                        retBuffer.clear();
                        hasToWrite = false;
                    }

                    break;

                case INCOMPLETE:
                    connection.requestRead();
                    return 0;

                case ERR:
                    if (hasToWrite) {
                        connection.requestWrite(retBuffer);
                        retBuffer.clear();
                    }
                    connection.requestClose();
            }

        }

        return 0;
    }


    private NegotiationStatus generateResp() throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("start document");


                case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                    System.out.println("processing instruction");

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

        return NegotiationStatus.ERR;

    }


    private NegotiationStatus handleEndElement() {

        if (reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")) {
            String ret = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">" + user64 + "</auth>";
            retBuffer = ByteBuffer.wrap(ret.getBytes());
            hasToWrite = true;
            sentAuth = true;
            return NegotiationStatus.IN_PROCESS;
        }
        if (reader.getName().equals("mechanisms")) {
            inMech = false;
        }

        if(sentAuth && tagClientResponse != null && tagClientResponse.equals(reader.getName())){
            StringBuilder closingTag = new StringBuilder("</");
//            if(!reader.getPrefix().equals("")){
//                closingTag.append(reader.getPrefix())
//            }

        }

        if (reader.getLocalName().equals("success") && hasPlain) {
            return NegotiationStatus.FINISHED;
        }

        //TODO pensar bien que resp:
        return NegotiationStatus.IN_PROCESS;

    }

    private NegotiationStatus handleStartElement() {

        if (reader.getLocalName().equals("mechanism")) {
            inMech = true;
        }

        if (reader.getLocalName().equals("message") || reader.getLocalName().equals("iq") || reader.getLocalName().equals("presence")) {
            return handleNotAuthorized();
        }


        return NegotiationStatus.IN_PROCESS;

    }

    /**Error Handlers**/

    //TODO:cerrar connection

    /**
     * RFC 4.9.3.1.  bad-format
     */
    private int handleWrongFormat(Connection connection) {
        connection.requestWrite(ByteBuffer.wrap("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
        System.out.println("ERROR DE XML DE NEGOCIACION ADENTRO DEL NEGOTIATOR SERVER");
        return -1;
        //TODO: cierro la connection como?

    }

    //TODO:cierro conenection!

    /**
     * RFC 4.9.3.12.  not-authorized
     */
    private NegotiationStatus handleNotAuthorized() {
        retBuffer.clear();
        retBuffer = ByteBuffer.wrap("<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return NegotiationStatus.ERR;
    }
}
