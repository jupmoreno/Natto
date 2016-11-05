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

    @Override
    public int handshake(Connection connection) {

        verificationState readResult = verificationState.INCOMPLETE;

        //aca hay que poner bien de quien es TODO
        //el primero mandar es el cliente, nosotros actuamos como cliente cuando negociamos como el servidor
        retBuffer.wrap(new String("<?xml version=\"1.0\"?>\n" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">").getBytes());
        //escribo en el servidor el byte buffer y lo limpio

        System.out.println("mando el tag de abrir <?xml version=\"1.0\"?>\n" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");

        retBuffer.clear();

        while(readResult != verificationState.FINISHED) {
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
            if(readResult == verificationState.FINISHED){
                System.out.println("termine negociacion :)");
                return 1;
            }
            if(readResult == verificationState.IN_PROCESS){

            }


        }

        return 0;
    }


    private verificationState generateResp() throws XMLStreamException {
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
                    return verificationState.INCOMPLETE;

                default:
                    break;
            }
        }

        ///????
        return verificationState.ERR;

    }


    private verificationState handleEndElement(){
        if(reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")){
            //TODO claramente AGFkbWluAGZyYW4xOTk0 no va hardcodeado CAMBIAR BIEN
            retBuffer.wrap(new String("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">AGFkbWluAGZyYW4xOTk0</auth>").getBytes());
            return verificationState.IN_PROCESS;
        }
        if(reader.getName().equals("mechanisms")){
            inMech = false;
        }

        //TODO pensar bien que resp:
        return verificationState.IN_PROCESS;

    }

    private verificationState handleStartElement(){
        if(reader.getLocalName().equals("success") && hasPlain){
            return verificationState.FINISHED;
        }
        if(reader.getLocalName().equals("mechanism")){
            inMech = true;
        }

        return verificationState.IN_PROCESS;

    }

}
