package ar.edu.itba.pdc.natto.protocol.xmpp;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

/**
 * Created by natinavas on 11/4/16.
 */
public class Negotiator {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer;
    private StringBuilder sb = new StringBuilder();


    //ver si devuelvo int o byte buffer si es que escribo adentro o afuera

    //si hago todo desde aca adentro
        //devuelvo -1 cuando es error y hay que cerrar todo bien
        //devuelvo 1 cuando ya esta verificado y hay que pasar a parsear por el xmpp parser


    private enum verificationState{
        FINISHED, INCOMPLETE, IN_PROCESS, ERR,
    }


    //voy a recibir una Connection a quien le requesteo escribir y leer
    public int handshake(ByteBuffer buffer){



        verificationState readResult = verificationState.INCOMPLETE;

        while(readResult != verificationState.FINISHED) {


            //leo cada vez no?


            if(reader.getInputFeeder().needMoreInput()){
                try {
                    reader.getInputFeeder().feedInput(buffer);
                    retBuffer.clear();
                } catch (XMLStreamException e) {
                    System.out.println("ERROR DE XML DE NEGOCIACION");
                    return -1;
                }
            }



            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                System.out.println("error del parseo del handshaking");
                return -1;
            }

            if (readResult == verificationState.FINISHED) {
                //aca ya termino de verificarse y devuelve uno para que el socket connection sepa que ya esta
                //tengo que tambien escribir la ultima cosa en el cliente

                sb.setLength(0);
                retBuffer.clear();
                return 1;


            } else if (readResult == verificationState.IN_PROCESS) {
                //convierto el sb a byte buffer y escribo en byte buffer para el cliente (COMO ESCRIBO EN EL CLEITNE!?)

                sb.setLength(0);
                retBuffer.clear();


            }else if (readResult == verificationState.INCOMPLETE){
                //tengo que leer mas
            }else if( readResult == verificationState.ERR){
                return -1;
            }
        }

        return 0;
    }

    private verificationState generateResp() throws XMLStreamException{

            while (reader.hasNext()) {
                switch (reader.next()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        handleStartDocument();
                        System.out.println("start document");
                        break;

                    case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:

                        break;

                    case AsyncXMLStreamReader.START_ELEMENT:
                        handleStartElement();
                        break;

                    case AsyncXMLStreamReader.CHARACTERS:

                        break;

                    case AsyncXMLStreamReader.END_ELEMENT:

                        break;

                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return verificationState.INCOMPLETE;

                    default:
                        break;
                }
            }

            return verificationState.ERR;
            ///????

    }

    private void handleStartDocument(){
        sb.append("<?xml ");

        if(reader.getVersion() != null){
            sb.append("version= '").append(reader.getVersion()).append("' ");
        }

        if(reader.getEncoding() == null){
            sb.append("encoding='UTF-8 ");
        }else{ //TODO porque no seria null???? ver si hay que tener este caso en cuenta
            sb.append("encoding=").append(reader.getVersion()).append(" ");
        }
        sb.append("?>");
    }

    private void handleStartElement(){
        String name = reader.getName().getLocalPart();
        //stream:Stream
        if(name.equals("stream") && reader.getName().getLocalPart().equals("stream")){


            //stream:features
        }else if(name.equals("stream") && reader.getName().getLocalPart().equals("features")){

        }
    }

}
