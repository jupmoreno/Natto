package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.ProtocolHandler;
import ar.edu.itba.pdc.natto.protocol.LinkedProtocolHandler;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

public class XmppParser extends ProtocolHandler implements LinkedProtocolHandler {
    private final static int BUFFER_MAX_SIZE = 10000;

    private final AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private final AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();

    private final ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_MAX_SIZE);
    private final XmppData xmppData;
    private final String user;

    private LinkedProtocolHandler link;

    private boolean inMessage = false;
    private boolean inBody = false;

    //   StringBuilder sb = new StringBuilder();

    public XmppParser(XmppData data, String user) {
        this.xmppData = data;
        this.user = user;
    }

    @Override
    public void link(LinkedProtocolHandler link) {
        this.link = link;
    }

    @Override
    public void requestRead() {
        connection.requestRead();
    }

    @Override
    public void requestWrite(ByteBuffer buffer) {
        int before = buffer.remaining();
        connection.requestWrite(buffer);
        xmppData.moreBytesTransferred(before - buffer.remaining());
        System.out.println("LA CANTIDAD DE BYTES QUE ESCRIBI SON DEL PARSER  " + (before - buffer.remaining()));
    }

    @Override
    public void finishedWriting() {
        if (retBuffer.hasRemaining()) {
            link.requestWrite(retBuffer);
        } else {
            retBuffer.clear();
            if (parser.getInputFeeder().needMoreInput()) {
                connection.requestRead();
            } else {
                int ret = parse();

                if (ret == -1) {
                    checkState(false);
                    // TODO:
                } else if (ret == 1) {
                    retBuffer.flip();
                    link.requestWrite(retBuffer);
                } else {
                    connection.requestRead();
                }
            }
        }
    }

    @Override
    public void requestClose() {
        connection.requestClose();
    }

    @Override
    public void afterConnect() {
        throw new IllegalStateException("Not a connectable handler");
    }

    @Override
    public void afterRead(ByteBuffer buffer) {
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
                return;
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
            return;
        }

        int ret = parse();

        if (ret == -1) {
            checkState(false);
            // TODO:
        } else if (ret == 1) {
            retBuffer.flip();
            link.requestWrite(retBuffer);
        } else {
            connection.requestRead();
        }
    }

    @Override
    public void afterWrite() {
        link.finishedWriting();
    }

    @Override
    public void beforeClose() {
        // TODO
    }

    private int parse() {
        try {
            while (parser.hasNext()) {
                switch (parser.next()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        handleStartDocument();
                        break;

                    case AsyncXMLStreamReader.START_ELEMENT:
                        handleStartElement();
                        return 1;

                    case AsyncXMLStreamReader.CHARACTERS:
                        handleCharacters();
                        return 1;

                    case AsyncXMLStreamReader.END_ELEMENT:
                        handleEndElement();
                        return 1;

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
        if (true) { // TODO: Remove
            return false;
        }

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
        String local = parser.getLocalName();
        String prefix = parser.getPrefix();

        if (local.equals("message")) {
            inMessage = true;
        } else if (local.equals("body") && inMessage) {
            inBody = true;
        }

        retBuffer.put("<".getBytes());
//        sb.append("<");
        if (prefix != null && !prefix.isEmpty()) {
            retBuffer.put(prefix.getBytes())
                    .put(":".getBytes());
//            sb.append(parser.getPrefix()).append(":");
        }
        retBuffer.put(local.getBytes());
//        sb.append(name);
        retBuffer.put(" ".getBytes());
//            sb.append(" ");

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (!parser.getAttributePrefix(i).isEmpty()) {
                retBuffer.put(parser.getAttributePrefix(i).getBytes())
                        .put(":".getBytes());
//                sb.append(parser.getAttributePrefix(i)).append(":");
            }

            retBuffer.put(parser.getAttributeLocalName(i).getBytes())
                    .put("='".getBytes())
                    .put(parser.getAttributeValue(i).getBytes())
                    .put("' ".getBytes());
//            sb.append(parser.getAttributeLocalName(i)).append("=\"").append(parser.getAttributeValue(i)).append("\"");
        }

        for (int i = 0; i < parser.getNamespaceCount(); i++) {
            retBuffer.put("xmlns".getBytes());
//            sb.append(" ").append("xmlns");
            if (!parser.getNamespacePrefix(i).isEmpty()) {
                retBuffer.put(":".getBytes())
                        .put(parser.getNamespacePrefix(i).getBytes());
//                sb.append(":").append(parser.getNamespacePrefix(i));
            }

            retBuffer.put("='".getBytes())
                    .put(parser.getNamespaceURI(i).getBytes())
                    .put("' ".getBytes());
//            sb.append("=\"").append(parser.getNamespaceURI(i)).append("\"");
        }

        retBuffer.put(XmppMessages.INITIAL_STREAM_END.getBytes());
//        sb.append(">");
    }

    public void handleCharacters() {
        if (inBody) {
            for (char c : parser.getText().toCharArray()) {
                transform(c);
            }
        } else {
            retBuffer.put(parser.getText().getBytes());
//            sb.append(parser.getText());
        }
    }

    private void transform(char c) {
        boolean changed = false;

        if (xmppData.isTransformEnabled()) {
            switch (c) {
                case 'a':
                    changed = true;
                    retBuffer.put("4".getBytes());
//                            sb.append("4");
                    break;

                case 'e':
                    changed = true;
                    retBuffer.put("3".getBytes());
//                            sb.append("3");
                    break;

                case 'i':
                    changed = true;
                    retBuffer.put("1".getBytes());
//                            sb.append("1");
                    break;

                case 'o':
                    changed = true;
                    retBuffer.put("0".getBytes());
//                            sb.append("0");
                    break;

                case 'c':
                    changed = true;
                    retBuffer.put("&lt;".getBytes());
//                            sb.append("&lt;");
                    break;
            }
        }

        if (!changed) {
            switch (c) {
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
                    retBuffer.put(String.valueOf(c).getBytes());
//                        sb.append(c);
                    break;
            }
        }
    }

    private void handleEndElement() {
        String local = parser.getLocalName();
        String prefix = parser.getPrefix();

        retBuffer.put("</".getBytes());
//        sb.append("</");}

        if (prefix != null && !prefix.isEmpty()) {
            retBuffer.put(parser.getPrefix().getBytes())
                    .put(":".getBytes());
//            sb.append(parser.getPrefix()).append(":");
        }

        retBuffer.put(local.getBytes())
                .put(">".getBytes());
//        sb.append(parser.getName().getLocalPart()).append(">");

        if (local.equals("body") && inMessage) {
            inBody = false;
        } else if (local.equals("message")) {
            inMessage = false;
        }
    }
}
