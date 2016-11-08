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
    private boolean hasToWrite = false;

    private boolean verified = false;

    private String user64;

    @Override
    public boolean isVerified() {
        return verified;
    }

    @Override
    public int handshake(Connection connection, ByteBuffer readBuffer) {

        VerificationState readResult = VerificationState.INCOMPLETE;

        System.out.println("ENTRO AL HANDSHAKE Y EL BUFFER QUE ME ENTRA ES " + new String(readBuffer.array(), readBuffer.position(), readBuffer.limit()));

        retBuffer = ByteBuffer.allocate(10000); //TODO SACAR

        if(reader.getInputFeeder().needMoreInput()){
            try {
                System.out.println("feedeo al buffer");
                System.out.println("EL BUFFER QUE FFEEEDEO ES "+ new String(readBuffer.array(), readBuffer.position(), readBuffer.limit()));
                reader.getInputFeeder().feedInput(readBuffer);
                retBuffer.clear();
            } catch (XMLStreamException e) {
                System.out.println("ERROR DE XML DE NEGOCIACION ADENTRO DEL NEGOTIATOR SERVER");
                System.out.println(e.getMessage());
                return -1;
            }
        }

        while(readResult != VerificationState.FINISHED) {



            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
            //    System.out.println("error en el generate resp de el negotiator con el server");
              //
                System.out.println(e.getMessage());
                return 1;
            }


            switch (readResult){
                case FINISHED:
                    System.out.println("TERMINO DE NEGOCIAR :)");
                    verified = true;
                    //TODO: habilitar que el cliente pueda escribir en el servidor
                   // connection.requestRead();  ???
                    return 1;

                case IN_PROCESS:
                    if(hasToWrite){
                        connection.requestWrite(retBuffer);
                        retBuffer = ByteBuffer.allocate(10000); //TODO SACAR DIUJ
                    }

                    break;

                case INCOMPLETE:
                    connection.requestRead();
                    return 0;
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
               //     System.out.println("start element");
                    return handleStartElement();

                case AsyncXMLStreamReader.CHARACTERS:
                    if(inMech && reader.getText().equals("PLAIN")){
                        hasPlain = true;
                    }
                    //System.out.println("characters");
                    //System.out.println("los characters que me llegan son " + reader.getText());
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    //System.out.println("end element");
                    return handleEndElement();

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    //System.out.println("incomplete");
                    return VerificationState.INCOMPLETE;

                default:
                    break;
            }
        }

        ///????
        return VerificationState.ERR;

    }


    private VerificationState handleEndElement(){
     //   System.out.println("en el end element el nombre del tag es " + reader.getLocalName());
        if(reader.getPrefix().equals("stream") && reader.getLocalName().equals("features")){
            String ret = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">" + user64 + "</auth>";
            retBuffer = ByteBuffer.wrap(ret.getBytes());
            hasToWrite = true;
            return VerificationState.IN_PROCESS;
        }
        if(reader.getName().equals("mechanisms")){
            inMech = false;
        }

        if(reader.getLocalName().equals("success")){
       //     System.out.println("ESTA ACA EN END ELEMENT");
        }
        //TODO pensar bien que resp:
        return VerificationState.IN_PROCESS;

    }

    private VerificationState handleStartElement(){
  //      System.out.println("en en elemente l nombre del tag es " + reader.getLocalName());

        if(reader.getLocalName().equals("success") && hasPlain){
    //        System.out.println("ESTA ACA EN START ELEMENT");
            return VerificationState.FINISHED;
        }
        if(reader.getLocalName().equals("mechanism")){
            inMech = true;
        }

        return VerificationState.IN_PROCESS;

    }

    public void setUser64(String user64) {
        this.user64 = user64;
    }
}
