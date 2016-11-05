package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Negotiator;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


public class NegotiatorClient implements Negotiator {

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader = inputF.createAsyncForByteBuffer();

    private ByteBuffer retBuffer = ByteBuffer.allocate(1000000);
  //  private StringBuilder sb = new StringBuilder();

    private boolean verified = false;


    //ver si devuelvo int o byte buffer si es que escribo adentro o afuera

    //si hago todo desde aca adentro
    //devuelvo -1 cuando es error y hay que cerrar todo bien
    //devuelvo 1 cuando ya esta verificado y hay que pasar a parsear por el xmpp parser

    //TODO
    @Override
    public boolean isVerified() {
        return verified;
    }

    //TODO appendear directo al ret buffer y no al sb (para testear es mas facil :) asi )
    //voy a recibir una Connection a quien le requesteo escribir y leer
    public int handshake(Connection connection, ByteBuffer readBuffer) {

        VerificationState readResult = VerificationState.INCOMPLETE;

        retBuffer.clear();
        while (readResult != VerificationState.FINISHED) {

          //  connection.requestRead();


            //TODO tengo que fijarme si necesito mas para leer o siempre le feedeo lo que me llega?
            try {
                reader.getInputFeeder().feedInput(readBuffer);
            } catch (XMLStreamException e) {
                System.out.println("error feedando al parser del client negotiator");
            }


            try {
                readResult = generateResp();
            } catch (XMLStreamException e) {
                System.out.println("error del parseo del handshaking");
                return -1;
            }

            if (readResult == VerificationState.FINISHED) {
                System.out.println("ESTADO TERMINADO: escribo en el connection");
             //   connection.requestWrite(retBuffer);

                System.out.println("lo que mando en FINISHED "+ new String(retBuffer.array(), Charset.forName("UTF-8")));

                System.out.println("lo ultimo que mando ");
                retBuffer.clear();
                verified = true;
                return 1;

            } else if (readResult == VerificationState.IN_PROCESS) {
                System.out.println("lo que mando en PROCESS "+ new String(retBuffer.array(), Charset.forName("UTF-8")));
              //  connection.requestWrite(retBuffer);
                retBuffer.clear();

            } else if (readResult == VerificationState.ERR) {
                System.out.println("ERROR");
                return -1;
            }
        }

        return 0;
    }

    private VerificationState generateResp() throws XMLStreamException {

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
            retBuffer.put("<?xml ".getBytes());

            if (reader.getVersion() != null) {
                retBuffer.put("version= '".getBytes()).put(reader.getVersion().getBytes()).put("' ".getBytes());
            }

            if (reader.getEncoding() == null) {
                retBuffer.put("encoding='UTF-8?>".getBytes());
            } else { //TODO porque no seria null???? ver si hay que tener este caso en cuenta
                retBuffer.put("encoding=".getBytes()).put(reader.getVersion().getBytes()).put("?>".getBytes());
            }
        }
        return VerificationState.IN_PROCESS;
    }


    private VerificationState handleStartElement() {

        String name = reader.getName().getLocalPart();

        //stream:Stream
        if (name.equals("stream") && reader.getPrefix().equals("stream")) {
            return handleStreamStream();

            //auth
        } else if (name.equals("auth")) {

            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (reader.getAttributeLocalName(i).equals("mechanism") && reader.getAttributeValue(i).equals("PLAIN")) {
                    //hay uqe meterle algo adentro, algo de ese estilo cnNwYXV0aD1mNDVhM2E2Y2NmYmE4MDVmOGFkNzk4MjU0ZGI5MzdmNw==  //base64
                    System.out.println("el mecanismo es PLAIN");

                    retBuffer.put("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"></success>".getBytes());
                    return VerificationState.FINISHED;
                }
            }
            //si no me llego lo de auth plain tengo que tirar error no?
            return VerificationState.ERR;
        }

        //VER UN POCO MAS?

        return VerificationState.ERR;
    }

    private VerificationState handleStreamStream() {
        retBuffer.put("<stream:stream ".getBytes());

        //TODO meter id ver como se hace
        //appendeo los atributos y cambio el to por un from, y el from por un to
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            retBuffer.put(" ".getBytes());
            if (!reader.getAttributePrefix(i).isEmpty()) {
                retBuffer.put(reader.getAttributePrefix(i).getBytes()).put(":".getBytes());
            }
            if (reader.getAttributeLocalName(i).equals("to")) {
                retBuffer.put("from=\"".getBytes()).put(reader.getAttributeLocalName(i).getBytes()).put("\"".getBytes());

            } else if (reader.getAttributeLocalName(i).equals("from")) {
                retBuffer.put("to=\"".getBytes()).put(reader.getAttributeLocalName(i).getBytes()).put("\"".getBytes());
            } else {
                retBuffer.put(reader.getAttributeLocalName(i).getBytes()).put("=\"".getBytes()).put(reader.getAttributeValue(i).getBytes()).put("\"".getBytes());
            }
        }

        //apendeo los namespaces
        appendNamespaces();

        //mecanismos de encriptacion
        retBuffer.put("><stream:features><starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls><mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">".getBytes());
        retBuffer.put("<mechanism>PLAIN</mechanism>".getBytes());

        //TODO: VER QUE HACER CON ESTO DE LOS ZIPS
        //mecanismos de compresion
        retBuffer.put("<compression xmlns=\"http://jabber.org/features/compress\">".getBytes());
        retBuffer.put("<method>zlib</method></compression>".getBytes());

        retBuffer.put("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>".getBytes());
        retBuffer.put("<register xmlns=\"http://jabber.org/features/iq-register\"/></stream:features>".getBytes());

        return VerificationState.IN_PROCESS;
    }


    private void appendNamespaces() {
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            retBuffer.put(" xmlns".getBytes());
            if (!reader.getNamespacePrefix(i).isEmpty()) {
                retBuffer.put(":".getBytes()).put(reader.getNamespacePrefix(i).getBytes());
            }
            retBuffer.put("=\"".getBytes()).put(reader.getNamespaceURI(i).getBytes()).put("\"".getBytes());
        }
    }

}
