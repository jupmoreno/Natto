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
import javax.xml.ws.soap.SOAPBinding;
import java.nio.ByteBuffer;

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

    //  private ByteBuffer clientResponse = ByteBuffer.allocate(100000);
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

        System.out.println("el buffer que me entra es " + new String(readBuffer.array(), readBuffer.position(), readBuffer.limit()));

        if (sentAuth) {
            System.out.println("meto en el client response el buffer " + new String(readBuffer.array(), readBuffer.position(), readBuffer.limit()));
            retBuffer.clear();
            retBuffer = readBuffer.duplicate();

            System.out.println("lo uqe tengo en el client response " + new String(retBuffer.array(), retBuffer.position(), retBuffer.position()));
        }

        int ret = handshake(me, other, readBuffer);


        System.out.println("salgo del handshake");
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
            System.out.println("requesteo leer");
            me.requestRead();
        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        me.requestRead();

    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }

    public int handshake(Connection me, Connection other, ByteBuffer readBuffer) {

        System.out.println("Entro a handshake");
        NegotiationStatus readResult = NegotiationStatus.INCOMPLETE;

        if (reader.getInputFeeder().needMoreInput()) {
            try {
                System.out.println("lo feedeo asique no deberia estar incomplete (?)");
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                return handleWrongFormat(me);
            }
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
                    System.out.println("in process");

                    if (hasToWrite) {
                        me.requestWrite(retBuffer);
                        System.out.println("EN HAS TO WRITE ESCRIBO  " + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));
                        hasToWrite = false;

                    } else if (haveToSendAuth) {
                        System.out.println("EN HAS TO WRITE AUTh  " + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));
                        System.out.println("estoy aca?");
                        me.requestWrite(retBuffer);
                        haveToSendAuth = false;
                        sentAuth = true;

                    } else if (sentAuth) {
                        System.out.println("EN HAS TO WRITE OTRO CASo  " + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));
                        other.requestWrite(retBuffer);
                    }

                    break;

                case INCOMPLETE:
                    System.out.println("incomplete");
                    return 0;

                case ERR:
                    System.out.println("error");
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
                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("start document");


                case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                    System.out.println("processing instruction");

                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    System.out.println("start element");
                    return handleStartElement();

                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.println("characters");
                    if (inMech && reader.getText().equals("PLAIN")) {
                        System.out.println("tiene plain");
                        hasPlain = true;
                    }
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("end element");
                    return handleEndElement();

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("entr oa ca a incomplete???");
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
            retBuffer.clear(); //TODO ?
            retBuffer = ByteBuffer.wrap(ret.getBytes());
            haveToSendAuth = true;
            System.out.println("mando el auth");
            return NegotiationStatus.IN_PROCESS;
        }
        if (reader.getName().equals("mechanisms")) {
            inMech = false;
        }

        if (sentAuth && tagClientResponse != null && tagClientResponse.equals(reader.getName().toString())) {
            if (reader.getLocalName().equals("success")) {
                return NegotiationStatus.FINISHED;
            }else{
                return NegotiationStatus.ERR;
            }

        }

        if (sentAuth && tagClientResponse == null) {
            if (reader.getLocalName().equals("success")) {
                return NegotiationStatus.FINISHED;
            }else{
                return NegotiationStatus.ERR;
            }
        }


//        if (reader.getLocalName().equals("success") && hasPlain) {
//            return NegotiationStatus.FINISHED;
//        }

        //TODO pensar bien que resp:
        return NegotiationStatus.IN_PROCESS;

    }

    private NegotiationStatus handleStartElement() {

        if (sentAuth && tagClientResponse == null) {
            System.out.println("el primer tag que le voy a mandar al cliente es " + reader.getName().toString());
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
