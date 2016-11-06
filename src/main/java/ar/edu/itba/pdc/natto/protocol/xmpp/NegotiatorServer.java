package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;


public class NegotiatorServer implements Negotiator {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(1000000);
    private StringBuilder sb = new StringBuilder();

    private boolean inMech = false;
    private boolean hasPlain = false;


    //TODO
    @Override
    public boolean isVerified() {
        return false;
    }

    @Override
    public int handshake(Connection connection, ByteBuffer readBuffer) {

        VerificationState readResult = VerificationState.INCOMPLETE;

        //aca hay que poner bien de quien es TODO
        //el primero mandar es el cliente, nosotros actuamos como cliente cuando negociamos como el servidor


        retBuffer.clear();

        while(readResult != VerificationState.FINISHED) {
            System.out.println("Entro al while");

//            if(reader.getInputFeeder().needMoreInput()){
//                try {
//                    //aca deberia leer no feedearle el buffer que recibe, no deberia recibir buffer
//                    reader.getInputFeeder().feedInput(buffer);
//                    retBuffer.clear();
//                } catch (XMLStreamException e) {
//                    System.out.println("ERROR DE XML DE NEGOCIACION ADENTRO DEL NEGOTIATOR SERVER");
//                    return -1;
//                }
//            }

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                System.out.println("error en el generate resp de el negotiator con el server");
            }

            //no tengo que escribir nada porque termina cuando recibe un success, no vuelve a mandar nada
            if(readResult == VerificationState.FINISHED){
                System.out.println("termine negociacion :)");
                return 1;
            }
            if(readResult == VerificationState.IN_PROCESS){

            }


        }

        return 0;
    }


    private VerificationState generateResp() throws XMLStreamException {
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
                    if(inMech && reader.getText().equals("PLAIN")){
                        hasPlain = true;
                    }
                    System.out.println("characters");
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("end element");
                    return handleEndElement();

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("incomplete");
                    return VerificationState.INCOMPLETE;

                default:
                    break;
            }
        }

        ///????
        return VerificationState.ERR;

    }


    private VerificationState handleEndElement(){
        if(reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")){
            //TODO claramente AGFkbWluAGZyYW4xOTk0 no va hardcodeado CAMBIAR BIEN
            retBuffer.wrap(new String("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">AGFkbWluAGZyYW4xOTk0</auth>").getBytes());
            return VerificationState.IN_PROCESS;
        }
        if(reader.getName().equals("mechanisms")){
            inMech = false;
        }

        //TODO pensar bien que resp:
        return VerificationState.IN_PROCESS;

    }

    private VerificationState handleStartElement(){
        if(reader.getLocalName().equals("success") && hasPlain){
            return VerificationState.FINISHED;
        }
        if(reader.getLocalName().equals("mechanism")){
            inMech = true;
        }

        return VerificationState.IN_PROCESS;

    }

}
