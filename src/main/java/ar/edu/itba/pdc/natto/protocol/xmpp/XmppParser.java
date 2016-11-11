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
        ByteBuffer ret = parse(buffer);
        if (error) {
            me.requestClose();
            //TODO: mandar bien mensaje de error ESTO ES ASI?
            other.requestClose();
        }
        other.requestWrite(ret);

    }

    @Override
    public void afterWrite(Connection me, Connection other) {
        other.requestRead();
    }

    @Override
    public void beforeClose(Connection me, Connection other) {

    }


    /*PARSER*/

    private ByteBuffer parse(ByteBuffer buffer) {

        System.out.println("me llega el byte buffer " + new String(buffer.array(), buffer.position(), buffer.limit()));
        if (buffer == null) {
            return null;
        }


        if (parser.getInputFeeder().needMoreInput()) {
            try {
                parser.getInputFeeder().feedInput(buffer);
                retBuffer.clear();

            } catch (XMLStreamException e) {
                return handleWrongFormat();
            }
        }

        /*Parsing Starts*/
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
//                        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
//                        sb.setLength(0);
                        return retBuffer;

                    case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                        break;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            return handleWrongFormat();

        }

        //TODO ?
//        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
//        sb.setLength(0);
        return retBuffer;
    }


    private void handleStartDocument() {
        if (parser.getVersion() == null && parser.getEncoding() == null) {
            return;
        }

        System.out.println("el buffer antes de ponerle nada en el start " + String.valueOf(StandardCharsets.UTF_8.decode(retBuffer)));
        System.out.println("el buffer idem "+ retBuffer);
//        retBuffer.put("<?xml ".getBytes());
//        sb.append("<?xml ");
        String version = parser.getVersion();
        String encoding = parser.getCharacterEncodingScheme();
        if (version != null)
            retBuffer.put("version=\"".getBytes());//.put(version.getBytes()).put("\" ".getBytes());
        System.out.println("el ret buffer " + retBuffer);
        System.out.println("Despeus de ponerle lo primero " + String.valueOf(StandardCharsets.UTF_8.decode(retBuffer)));
        // sb.append("version=\"" + version + "\" ");
        if (encoding != null) {
            retBuffer.put("encoding=\"".getBytes()).put(encoding.getBytes()).put("\"".getBytes());
            //sb.append("encoding=\"" + encoding + "\"");

        }
        System.out.println("despues de ponerle lo segundo " +String.valueOf(StandardCharsets.UTF_8.decode(retBuffer)));

        retBuffer.put("?>".getBytes());
//        sb.append("?>");

    }

    private void handleStartElement() {
        String name = parser.getName().getLocalPart().toString();
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


    /**Error Handlers**/

    /**
     * RFC 4.9.3.1.  bad-format
     */
    private ByteBuffer handleWrongFormat() {
        error = true;
//        sb.setLength(0);

        /* RFC 4.9.1.2. If the error is triggered by the initial stream header, the receiving entity MUST still send the opening <stream> tag*/
        if (initialSetup) {
            retBuffer.put("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>".getBytes());
//            sb.append("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>");
        }
        retBuffer.put("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>".getBytes());
//        sb.append("<stream:error><bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error></stream:stream>");
//        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
//        sb.setLength(0);
        return retBuffer;
    }

}
