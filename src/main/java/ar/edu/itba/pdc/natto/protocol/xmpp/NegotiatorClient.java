package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.omg.CORBA.INV_POLICY;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class NegotiatorClient implements Negotiator {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    //   private ByteBuffer retBuffer = ByteBuffer.allocate(10000);

    private boolean verified = false;
    private boolean inAuth = false;
    private StringBuilder sb = new StringBuilder();

    @Override
    public boolean isVerified() {
        return verified;
    }

    public int handshake(Connection connection, ByteBuffer readBuffer) {
        sb.setLength(0);

        System.out.println("ENTRO AL HANDSHAKE Y EL BUFFER QUE ME ENTRA ES " + new String(readBuffer.array(), readBuffer.position(), readBuffer.limit()));

        VerificationState readResult = VerificationState.INCOMPLETE;
//        retBuffer = ByteBuffer.allocate(10000); // TODO SACAR ES UN ASCO PERO ALGO ANDA MAL SIN ESTO VER MALDITO BYTE BUFFER!!!!!!!

        //TODO tengo que fijarme si necesito mas para leer o siempre le feedeo lo que me llega?


        if (reader.getInputFeeder().needMoreInput()) {
            try {
                reader.getInputFeeder().feedInput(readBuffer);

            } catch (XMLStreamException e) {
                System.out.println(e.getMessage());
                System.out.println("error feedando al parser del client negotiator");
            }
        }

        while (readResult != VerificationState.FINISHED) {

            //connection.requestRead();

            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                System.out.println(e.getMessage());
                System.out.println("error del parseo del handshaking");
                return -1;
            }


            switch (readResult) {
                case FINISHED:

                    System.out.println("ESTADO TERMINADO: escribo en el connection");
                    connection.requestWrite(ByteBuffer.wrap(sb.toString().getBytes()));
                    verified = true;
                    return 1;


                    //   System.out.println("lo que mando en FINISHED " + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));

                    //System.out.println("lo ultimo que mando ");
                    // retBuffer.clear();

                    // retBuffer = ByteBuffer.allocate(10000); //TODO SACAR
                    //       System.out.println("lo que mando en PROCESS despesu de limpar" + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));


                case IN_PROCESS:
                    connection.requestWrite(ByteBuffer.wrap(sb.toString().getBytes()));
                    sb.setLength(0);
                    break;
                    // retBuffer = ByteBuffer
                // .allocate(10000); //TODO SACAR
                    //retBuffer.clear();
                    //     System.out.println("lo que mando en PROCESS despus de limpiar" + new String(retBuffer.array(), retBuffer.position(), retBuffer.limit()));

                case ERR:
                    System.out.println("ERROR");
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
                    System.out.println("start document");
                    VerificationState vs = handleStartDocument(); //TODO SACAR
                    if (vs != null) {
                        return vs;
                    }
                    break;


                case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                    System.out.println("processing instruction");

                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    System.out.println(reader.getLocalName());
                    System.out.println("start element");
                    if(reader.getLocalName().equals("stream") && reader.getPrefix().equals("stream")){
                        return handleStreamStream();
                    }
                    handleStartElement();
                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    if(inAuth){
                        getUser();
                    }
                    System.out.println("characters");

                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    if(reader.getLocalName().equals("auth"))
                        return VerificationState.FINISHED;
                    return VerificationState.IN_PROCESS;
                  //  break;

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

    private VerificationState handleStartDocument() {
        if (reader.getVersion() != null && reader.getEncoding() != null) { //TODO: SACAR solo para testear no deberia pasar esto
            //  retBuffer.put("<?xml ".getBytes());
            sb.append("<?xml ");
            if (reader.getVersion() != null) {
                // retBuffer.put("version='".getBytes()).put(reader.getVersion().getBytes()).put("' ".getBytes());
                sb.append("version='").append(reader.getVersion()).append("' ");
            }

            if (reader.getEncoding() == null) {
                // retBuffer.put("encoding='UTF-8?>".getBytes());
                sb.append("encoding='UTF-8?>");
            } else { //TODO porque no seria null???? ver si hay que tener este caso en cuenta
                sb.append("encoding='").append(reader.getVersion()).append("'?>");
                //retBuffer.put("encoding=".getBytes()).put(reader.getVersion().getBytes()).put("?>".getBytes());
            }
            return VerificationState.IN_PROCESS;
        }
        return null;

    }


    private VerificationState handleStartElement() {

        String name = reader.getName().getLocalPart();

        //stream:Stream
        if (name.equals("stream") && reader.getPrefix().equals("stream")) {
            return handleStreamStream();

            //auth
        } else if (name.equals("auth")) {
            System.out.println("Estoy en auth");
            inAuth = true;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (reader.getAttributeLocalName(i).equals("mechanism") && reader.getAttributeValue(i).equals("PLAIN")) {
                    //hay uqe meterle algo adentro, algo de ese estilo cnNwYXV0aD1mNDVhM2E2Y2NmYmE4MDVmOGFkNzk4MjU0ZGI5MzdmNw==  //base64
                    System.out.println("el mecanismo es PLAIN");

                    sb.append("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>");
                    //retBuffer.put("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>".getBytes());
                    return VerificationState.FINISHED;
                }
            }
            return VerificationState.ERR;
        }

        //TODO VER UN POCO MAS?

        return VerificationState.ERR;
    }

    private VerificationState handleStreamStream() {


        System.out.println("entro al handle stream stream");
        sb.append("<stream:stream");
        //  retBuffer.put("<stream:stream ".getBytes());

        //TODO meter id ver como se hace
        //appendeo los atributos y cambio el to por un from, y el from por un to
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            sb.append(" ");
            // retBuffer.put(" ".getBytes());
            if (!reader.getAttributePrefix(i).isEmpty()) {
                sb.append(reader.getAttributePrefix(i)).append(":");
                // retBuffer.put(reader.getAttributePrefix(i).getBytes()).put(":".getBytes());
            }
            if (reader.getAttributeLocalName(i).equals("to")) {
                sb.append("from=\"").append(reader.getAttributeLocalName(i)).append("\"");
                //retBuffer.put("from=\"".getBytes()).put(reader.getAttributeLocalName(i).getBytes()).put("\"".getBytes());

            } else if (reader.getAttributeLocalName(i).equals("from")) {
                sb.append("to=\"").append(reader.getAttributeLocalName(i)).append("\"");
                // retBuffer.put("to=\"".getBytes()).put(reader.getAttributeLocalName(i).getBytes()).put("\"".getBytes());
            } else {
                sb.append(reader.getAttributeLocalName(i)).append("=\"").append(reader.getAttributeValue(i)).append("\"");
                // retBuffer.put(reader.getAttributeLocalName(i).getBytes()).put("=\"".getBytes()).put(reader.getAttributeValue(i).getBytes()).put("\"".getBytes());
            }
        }

        //apendeo los namespaces
        appendNamespaces();

        //mecanismos de encriptacion
        sb.append("><stream:features><starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        sb.append("<mechanism>PLAIN</mechanism>");
        sb.append("</mechanisms>");

//        retBuffer.put("><stream:features><starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">".getBytes());
//        retBuffer.put("<mechanism>PLAIN</mechanism>".getBytes());

        //TODO: VER QUE HACER CON ESTO DE LOS ZIPS
        //mecanismos de compresion
        sb.append("<compression xmlns=\"http://jabber.org/features/compress\">");
        sb.append("<method>zlib</method></compression>");

//        retBuffer.put("<compression xmlns=\"http://jabber.org/features/compress\">".getBytes());
//        retBuffer.put("<method>zlib</method></compression>".getBytes());

        sb.append("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>");
        sb.append("<register xmlns=\"http://jabber.org/features/iq-register\"/></stream:features>");
//        retBuffer.put("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>".getBytes());
//        retBuffer.put("<register xmlns=\"http://jabber.org/features/iq-register\"/></stream:features>".getBytes());

        return VerificationState.IN_PROCESS;
    }


    private void appendNamespaces() {
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            sb.append("xmlns");
            //retBuffer.put(" xmlns".getBytes());
            if (!reader.getNamespacePrefix(i).isEmpty()) {
                sb.append(":").append(reader.getNamespacePrefix(i));
                //retBuffer.put(":".getBytes()).put(reader.getNamespacePrefix(i).getBytes());
            }
            sb.append("=\"").append(reader.getNamespaceURI(i)).append("\" ");
            //retBuffer.put("=\"".getBytes()).put(reader.getNamespaceURI(i).getBytes()).put("\" ".getBytes());
        }
    }

    private void getUser(){
        String user64 = reader.getText();
        System.out.println("El user es: " + user64);
        System.out.println("El user decodificado es: " + Base64.getDecoder().decode(user64));
    }

}
