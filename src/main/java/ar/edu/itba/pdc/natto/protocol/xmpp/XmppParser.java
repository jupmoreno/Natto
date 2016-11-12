package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("Duplicates") // TODO: Remove
public class XmppParser implements ProtocolHandler {

    private final static int BUFFER_MAX_SIZE = 10000;

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();


    private XmppData xmppData;

    private boolean inMessage = false;
    private boolean inBody = false;
    private boolean initialSetup = true;
    private boolean error = false;


    ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_MAX_SIZE);
    //   StringBuilder sb = new StringBuilder();


    public XmppParser(XmppData data) {
        this.xmppData = data;
    }


    @Override
    public void afterConnect(Connection me, Connection other) {
        checkState(false);
    }

    @Override
    public void afterRead(Connection me, Connection other, ByteBuffer buffer) {
        int ret = parse(buffer);

        if (ret == -1) {
            me.requestClose();
            //TODO: mandar bien mensaje de error ESTO ES ASI?
            other.requestClose();
        } else if (ret == 1) {
            // TODO
        } else {
            // TODO
        }

        // TODO Ver
        retBuffer.flip();
        other.requestWrite(retBuffer);

    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        if (retBuffer.hasRemaining()) {
            me.requestWrite(retBuffer);
        } else {
            me.requestRead();
        }
    }

    @Override
    public void beforeClose(Connection me, Connection other) {
        // TODO
    }

    /**
     * PARSER METHODS
     */

    private int parse(ByteBuffer buffer) {
        // TODO: Esto puede estar haciendo cualca
        buffer.mark();
        System.out.println("Me llega el byte buffer " + StandardCharsets.UTF_8.decode(buffer));
        buffer.rewind();

        if (parser.getInputFeeder().needMoreInput()) {
            try {
                parser.getInputFeeder().feedInput(buffer);
            } catch (XMLStreamException e) {
                // if the state is such that this method should not be called (has not yet
                // consumed existing input data, or has been marked as closed)
                // TODO: This should never happen
                checkState(false);

                // TODO
                // Al cliente XmppErrors.INTERNAL_SERVER
                // Al servidor </stream:stream>
                return -1;
            }
        } else {
            // Method called to check whether it is ok to feed more data: parser returns true if
            // it has no more content to parse (and it is ok to feed more); otherwise false
            // (and no data should yet be fed).
            // TODO: This should never happen
            checkState(false);
            // TODO
            // Al cliente XmppErrors.INTERNAL_SERVER
            // Al servidor </stream:stream>
            return -1;
        }

        try {
            while (parser.hasNext()) {
                switch (parser.next()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        handleStartDocument();
                        break;

                    case AsyncXMLStreamReader.START_ELEMENT:
                        handleStartElement();
                        break;

                    case AsyncXMLStreamReader.CHARACTERS:
                        handleCharacters();
                        break;

                    case AsyncXMLStreamReader.END_ELEMENT:
                        handleEndElement();
                        break;

                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return 0;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            // Al cliente XmppErrors.BAD_FORMAT
            // Al servidor </stream:stream>
            return -1;

        }

        // TODO: Acordarse de hacer el endOfInput cuando recibe </stream:stream>
        // Esta en estado END_DOCUMENT
        return 1;
    }


    private boolean handleStartDocument() {
        String version = parser.getVersion();
        String encoding = parser.getEncoding();

        // TODO Mandarlo siempre no?
//        if (version == null && encoding == null) {
//            return true;
//        }

        if (encoding != null && !encoding.equals("UTF-8")) {
            // TODO:
            // Al cliente XmppErrors.UNSUPPORTED_ENCODING
            // Al servidor cerrar conexion
            return false;
        }

        retBuffer.put(XmppMessages.VERSION_AND_ENCODING.getBytes());

        return true;
    }

    private void handleStartElement() {
        String name = parser.getName().getLocalPart();

        if (name.equals("stream") && parser.getPrefix().equals("stream")) {
            initialSetup = false;
        }

        if (name.equals("message")) {
            inMessage = true;
        } else if (name.equals("body") && inMessage) {
            inBody = true;
        }
        retBuffer.put("<".getBytes());
//        sb.append("<");
        if (parser.getPrefix().length() != 0) {
            retBuffer.put(parser.getPrefix().getBytes()).put(":".getBytes());
//            sb.append(parser.getPrefix()).append(":");
        }
        retBuffer.put(name.getBytes());
//        sb.append(name);


        for (int i = 0; i < parser.getAttributeCount(); i++) {
            retBuffer.put(" ".getBytes());
//            sb.append(" ");
            if (!parser.getAttributePrefix(i).isEmpty()) {
                retBuffer.put(parser.getAttributePrefix(i).getBytes()).put(":".getBytes());
//                sb.append(parser.getAttributePrefix(i)).append(":");
            }
            retBuffer.put(parser.getAttributeLocalName(i).getBytes()).put("=\"".getBytes()).put(parser.getAttributeValue(i).getBytes()).put("\"".getBytes());
//            sb.append(parser.getAttributeLocalName(i)).append("=\"").append(parser.getAttributeValue(i)).append("\"");
        }


        for (int i = 0; i < parser.getNamespaceCount(); i++) {
            retBuffer.put(" ".getBytes()).put("xmlns".getBytes());
//            sb.append(" ").append("xmlns");
            if (!parser.getNamespacePrefix(i).isEmpty()) {
                retBuffer.put(":".getBytes()).put(parser.getNamespacePrefix(i).getBytes());
//                sb.append(":").append(parser.getNamespacePrefix(i));
            }

            retBuffer.put("=\"".getBytes()).put(parser.getNamespaceURI(i).getBytes()).put("\"".getBytes());
//            sb.append("=\"").append(parser.getNamespaceURI(i)).append("\"");
        }

        retBuffer.put(">".getBytes());
//        sb.append(">");
    }

    public void handleCharacters() {

        if (inBody) { //TODO: se leetea?
            for (char c : parser.getText().toCharArray()) {
                switch (c) {
                    case 'a':
                        if (xmppData.isTransformEnabled())
                            retBuffer.put("4".getBytes());
//                            sb.append("4");
                        else
                            retBuffer.put("a".getBytes());
//                            sb.append(c);
                        break;
                    case 'e':
                        if (xmppData.isTransformEnabled())
                            retBuffer.put("3".getBytes());
//                            sb.append("3");
                        else
                            retBuffer.put("e".getBytes());
//                            sb.append(c);
                        break;
                    case 'i':
                        if (xmppData.isTransformEnabled())
                            retBuffer.put("1".getBytes());
//                            sb.append("1");
                        else
                            retBuffer.put("i".getBytes());
//                            sb.append(c);
                        break;
                    case 'o': ///TODO POR ACAAA

                        if (xmppData.isTransformEnabled())
                            retBuffer.put("0".getBytes());
//                            sb.append("0");
                        else
                            retBuffer.put("o".getBytes());
//                            sb.append(c);.
                        break;
                    case 'c':
                        if (xmppData.isTransformEnabled())
                            retBuffer.put("&lt;".getBytes());
//                            sb.append("&lt;");

                        else
                            retBuffer.put("c".getBytes());
//                            sb.append(c);
                        break;
                    case '<':
                        retBuffer.put("&lt;".getBytes());
//                        sb.append("&lt;");

                        break;
                    case '>':
                        retBuffer.put("&gt;".getBytes());
//                        sb.append("&gt;");
                        break;
                    case '&':
                        retBuffer.put("&amp;".getBytes());
//                        sb.append("&amp;");
                        break;
                    case '\'':
                        retBuffer.put("&apos;".getBytes());
//                        sb.append("&apos;");
                        break;
                    case '\"':
                        retBuffer.put("&quot;".getBytes());
//                        sb.append("&quot;");
                        break;
                    default:
                        retBuffer.putChar(c);
//                        sb.append(c);
                        break;
                }
            }
        } else

        {
            retBuffer.put(parser.getText().getBytes());
//            sb.append(parser.getText());
        }

    }

    private void handleEndElement() {
        retBuffer.put("</".getBytes());
//        sb.append("</");
        if (parser.getPrefix().length() != 0) {
            retBuffer.put(parser.getPrefix().getBytes()).put(":".getBytes());
//            sb.append(parser.getPrefix()).append(":");
        }
        retBuffer.put(parser.getName().getLocalPart().getBytes()).put(">".getBytes());
//        sb.append(parser.getName().getLocalPart()).append(">");


        if (parser.getName().getLocalPart().equals("body"))
            inBody = false;
        if (parser.getName().getLocalPart().equals("message"))
            inMessage = false;

    }
}
