package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;


public class NegotiatorClient implements Negotiator{

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(1000000);
    private StringBuilder sb = new StringBuilder();


    //ver si devuelvo int o byte buffer si es que escribo adentro o afuera

    //si hago todo desde aca adentro
    //devuelvo -1 cuando es error y hay que cerrar todo bien
    //devuelvo 1 cuando ya esta verificado y hay que pasar a parsear por el xmpp parser


    //TODO appendear directo al ret buffer y no al sb (para testear es mas facil :) asi )
    //voy a recibir una Connection a quien le requesteo escribir y leer
    public int handshake(Connection connection){

        verificationState readResult = verificationState.INCOMPLETE;

        while(readResult != verificationState.FINISHED) {

            connection.requestRead();
            //Y AHORA!??!!?!


            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                System.out.println("error del parseo del handshaking");
                return -1;
            }

            if (readResult == verificationState.FINISHED) {
                System.out.println("ESTADO TERMINADO: escribo en el connection");
                retBuffer.wrap(sb.toString().getBytes());
                connection.requestWrite(retBuffer);
                System.out.println("lo ultimo que mando " + sb);
                sb.setLength(0);
                retBuffer.clear();
                return 1;

            } else if (readResult == verificationState.IN_PROCESS) {
                System.out.println("el sb en proceso " + sb);
                retBuffer.wrap(sb.toString().getBytes());
                connection.requestWrite(retBuffer);
                sb.setLength(0);
                retBuffer.clear();

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
                    System.out.println("start document");
                    return handleStartDocument();


                case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                    System.out.println("processing instruction");

                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    System.out.println("start element");
                    return handleStartElement();

                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.println("characters");

                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("end element");
                    break;

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

    private verificationState handleStartDocument(){
        if(reader.getVersion() != null && reader.getEncoding() != null){ //TODO: SACAR solo para testear no deberia pasar esto
            sb.append("<?xml ");

            if(reader.getVersion() != null){
                sb.append("version= '").append(reader.getVersion()).append("' ");
            }

            if(reader.getEncoding() == null){
                sb.append("encoding='UTF-8?>");
            }else{ //TODO porque no seria null???? ver si hay que tener este caso en cuenta
                sb.append("encoding=").append(reader.getVersion()).append("?>");
            }
        }
        return verificationState.IN_PROCESS;
    }


    private verificationState handleStartElement(){

        String name = reader.getName().getLocalPart();

        //stream:Stream
        if(name.equals("stream") && reader.getPrefix().equals("stream")){
            return handleStreamStream();

            //auth
        }else if(name.equals("auth")){

            for(int i = 0; i < reader.getAttributeCount(); i++){
                if(reader.getAttributeLocalName(i).equals("mechanism") && reader.getAttributeValue(i).equals("PLAIN")){
                    //hay uqe meterle algo adentro, algo de ese estilo cnNwYXV0aD1mNDVhM2E2Y2NmYmE4MDVmOGFkNzk4MjU0ZGI5MzdmNw==  //base64
                    System.out.println("el mecanismo es PLAIN");

                    sb.append("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>");
                    return verificationState.FINISHED;
                }
            }
            //si no me llego lo de auth plain tengo que tirar error no?
            return verificationState.ERR;
        }

        //VER UN POCO MAS?

        return verificationState.ERR;
    }

    private verificationState handleStreamStream(){
        sb.append("<stream:stream ");

        //TODO meter id ver como se hace
        //appendeo los atributos y cambio el to por un from, y el from por un to
        for(int i = 0; i < reader.getAttributeCount(); i++){
            sb.append(" ");
            if (!reader.getAttributePrefix(i).isEmpty()) {
                sb.append(reader.getAttributePrefix(i)).append(":");
            }
            if(reader.getAttributeLocalName(i).equals("to")){
                sb.append("from=\"").append(reader.getAttributeName(i)).append("\"");
            }else if(reader.getAttributeLocalName(i).equals("from")){
                sb.append("to=\"").append(reader.getAttributeName(i)).append("\"");
            }else{
                sb.append(reader.getAttributeLocalName(i)).append("=\"").append(reader.getAttributeValue(i)).append("\"");
            }
        }

        //apendeo los namespaces
        appendNamespaces();

        //mecanismos de encriptacion
        sb.append("><stream:features><starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        sb.append("<mechanism>PLAIN</mechanism>");

        //TODO: VER QUE HACER CON ESTO DE LOS ZIPS
        //mecanismos de compresion
        sb.append("<compression xmlns=\"http://jabber.org/features/compress\">");
        sb.append("<method>zlib</method></compression>");

        sb.append("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>");
        sb.append("<register xmlns=\"http://jabber.org/features/iq-register\"/>");
        sb.append("</stream:features>");

        return verificationState.IN_PROCESS;
    }


    private void appendNamespaces(){
        for(int i = 0; i < reader.getNamespaceCount(); i++){
            sb.append(" ").append("xmlns");
            if (!reader.getNamespacePrefix(i).isEmpty()) {
                sb.append(":").append(reader.getNamespacePrefix(i));
            }
            sb.append("=\"").append(reader.getNamespaceURI(i)).append("\"");
        }
    }

}
