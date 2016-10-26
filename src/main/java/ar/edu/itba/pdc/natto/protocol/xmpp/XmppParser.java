package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.*;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

// TODO: Fijarse de siempre cerrar bien el parser anterior!
public class XmppParser implements Parser<Tag> {

    private String message = "<iqqq:iq xmlns:iqqq=\"holaaaaaa\"><hola></hola></iqqq:iq>";
    private ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());

    private final static int BUFFER_MAX_SIZE = 10000;

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;
    private Queue<ByteBuffer> buffers = new LinkedList<>();
    private Deque<Tag> tagDequeue = new LinkedList<>();

    private boolean newTag = true;

    public XmppParser() {

    }

    private int sizeBuffers() {
        int i = 0;
        for (ByteBuffer b : buffers) {
            i += b.remaining();
        }
        return i;
    }

    @Override
    public Tag fromByteBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        if (newTag) {
            parser = inputF.createAsyncForByteBuffer();
            newTag = false;
        }

        if (sizeBuffers() + buffer.remaining() > BUFFER_MAX_SIZE) {
            // TODO: Falta cerrar bien el parser
            return handleTooBig();
        }

        // Agrego el buffer para devolverlo en el caso de que no lo modifique
        buffers.add(buffer);

        try {
            parser.getInputFeeder().feedInput(buffer);
        } catch (XMLStreamException e) {
            // TODO: Forwardear o que hacer? ???
            e.printStackTrace();
            newTag = true;
            // TODO: Falta cerrar bien el parser
            return null; // TODO: ?
        }

        // Aca empieza la etapa de parseo
        int type = 0; // TODO: El primer caso pq es 0?

        Tag stanza = null;

        do {
            Tag tag = null;
            System.out.println("El tamaño de la pila de tags es: " + tagDequeue.size());

            switch (type) {

                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("start document");
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    if (tagDequeue.size() == 0) {
                        if (parser.getName().getLocalPart().toString().equals("iq")) {
                            tag = new Iq();
                        } else if (parser.getName().getLocalPart().toString().equals("presence")) {
                            tag = new Presence();
                        } else if (parser.getName().getLocalPart().toString().equals("message")) {
                            tag = new Message();
                        } else if (parser.getName().getLocalPart().toString().equals("stream")) {
                            tag = new Stream();
                            addAttributes(tag);
                            tag.setPrefix(parser.getPrefix());
                            tag.addNamespace(parser.getName().getNamespaceURI());

                            newTag = true;
                            // TODO: Falta cerrar bien el parser
                            return tag;

                        } else {
                            tag = new Tag(parser.getName().getLocalPart(), false);
                        }

                    } else {
                        boolean empty = true;

                        try {
                            empty = parser.isEmptyElement();
                        } catch (XMLStreamException e) {
                            // TODO: Que hacer?
                            e.printStackTrace();
                        }

                        tag = new Tag(parser.getName().getLocalPart(), empty);
                        tagDequeue.peek().addTag(tag);

                    }

                    addAttributes(tag);
                    tag.setPrefix(parser.getPrefix());
                    tag.addNamespace(parser.getName().getNamespaceURI());
                    tagDequeue.push(tag);
                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    tag = tagDequeue.poll();
                    tag.setValue(parser.getText());
                    tagDequeue.push(tag);
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    if (tagDequeue.size() == 1) {
                        stanza = tagDequeue.poll();
                    }

                    tagDequeue.poll();
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("incomplete");

                    return stanza; // TODO: Aca tiene que retornar null

                default:
                    break;
            }

            try {
                type = parser.next();
            } catch (com.fasterxml.aalto.WFCException e) {
                // TODO: Que hacer?
                return handleWrongFormat(tag);
            } catch (XMLStreamException e) {
                // TODO: Que hacer?
                e.printStackTrace();
                return null; // TODO: Que retornar?
            }

        } while (type != AsyncXMLStreamReader.END_DOCUMENT);

        System.out.println(stanza);

        newTag = true;
        // TODO: Falta cerrar bien el parser

        return stanza;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {
        if ((!message.isModified()) || message.isWrongFormat()) {
            ByteBuffer ret = ByteBuffer.allocate(sizeBuffers());
            while (!buffers.isEmpty()) {
                ret.put(buffers.poll());
            }
            ret.flip();
            System.out.println("LLegue aca, y el byteBuffer que devuelvo es de " + ret.remaining());
            return ret;
        }

        return ByteBuffer.wrap(message.toString().getBytes());
    }

    private void addAttributes(Tag tag) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            tag.addAttribute(parser.getAttributeName(i).toString(), parser.getAttributeValue(i));
        }
    }

    private Tag handleWrongFormat(Tag tag) {
        while (tagDequeue.size() > 1) {
            tagDequeue.poll();
        }

        if (tagDequeue.size() == 1) {
            tag = tagDequeue.poll();
            tag.setWrongFormat(true);
            return tag;
        }

        Tag retTag = new Tag("", true);
        retTag.setWrongFormat(true);

        return retTag;
    }

    private Tag handleTooBig() {
        while (tagDequeue.size() > 1) {
            tagDequeue.poll();
        }

        if (tagDequeue.size() == 1) {
            Tag tag = tagDequeue.poll();
            tag.setTooBig(true);
            return tag;
        }

        Tag retTag = new Tag("", true);
        retTag.setTooBig(true);

        return retTag;
    }
}
