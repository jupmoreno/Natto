package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

public class XmppServerNegotiator implements ProtocolHandler {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(100000);

    private boolean inMech = false;
    private boolean hasPlain = false;
    private boolean hasToWrite = false;

    private boolean verified = false;
    private String user64;

    @Override
    public void afterConnect(Connection me, Connection other) {

    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer readBuffer) {
        int ret = handshake(me, readBuffer);

        if (ret == -1) {
            // TODO Error
            me.requestClose();
            // TODO: Mandarle algo al cliente y cerrarlo
        } else if (ret == 1) {
            // TODO: crear nuevos handlers
            me.setHandler(null);
            // TODO: JP!
            other.setHandler(null);
        }
    }

    @Override
    public void afterWrite(Connection me, Connection other) {

    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }

    public int handshake(Connection connection, ByteBuffer readBuffer) {

        NegotiationStatus readResult = NegotiationStatus.INCOMPLETE;

        if(reader.getInputFeeder().needMoreInput()){
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }
        }

        while(readResult != NegotiationStatus.FINISHED) {

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                return handleWrongFormat(connection);
            }


            switch (readResult){
                case FINISHED:
                    verified = true;
                    retBuffer.clear();
                    //TODO: habilitar que el cliente pueda escribir en el servidor
                    return 1;

                case IN_PROCESS:
                    if(hasToWrite){
                        connection.requestWrite(retBuffer);
                        retBuffer.clear();
                        hasToWrite = false;
                    }

                    break;

                case INCOMPLETE:
                    connection.requestRead();
                    return 0;

                case ERR:
                    if(hasToWrite){
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
                    if(inMech && reader.getText().equals("PLAIN")){
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


    private NegotiationStatus handleEndElement(){

        if(reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")){
            String ret = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">" + user64 + "</auth>";
            retBuffer = ByteBuffer.wrap(ret.getBytes());
            hasToWrite = true;
            return NegotiationStatus.IN_PROCESS;
        }
        if(reader.getName().equals("mechanisms")){
            inMech = false;
        }

        if(reader.getLocalName().equals("success") && hasPlain){
            return NegotiationStatus.FINISHED;
        }

        //TODO pensar bien que resp:
        return NegotiationStatus.IN_PROCESS;

    }

    private NegotiationStatus handleStartElement(){

        if(reader.getLocalName().equals("mechanism")){
            inMech = true;
        }

        if(reader.getLocalName().equals("message") || reader.getLocalName().equals("iq") || reader.getLocalName().equals("presence")){
            return handleNotAuthorized();
        }


        return NegotiationStatus.IN_PROCESS;

    }

    /**Error Handlers**/

    //TODO:cerrar connection
    /**
     * RFC 4.9.3.1.  bad-format
     */
    private int handleWrongFormat(Connection connection){
        connection.requestWrite(ByteBuffer.wrap("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes()));
        System.out.println("ERROR DE XML DE NEGOCIACION ADENTRO DEL NEGOTIATOR SERVER");
        return -1;
        //TODO: cierro la connection como?

    }

    //TODO:cierro conenection!
    /**
     * RFC 4.9.3.12.  not-authorized
     */
    private NegotiationStatus handleNotAuthorized(){
        retBuffer.clear();
        retBuffer = ByteBuffer.wrap("<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
        hasToWrite = true;
        return NegotiationStatus.ERR;
    }
}