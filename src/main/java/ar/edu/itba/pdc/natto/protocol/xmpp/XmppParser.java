package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Auth;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Message;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Stream;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

// TODO: Fijarse de siempre cerrar bien el parser anterior!
public class XmppParser implements Parser<Tag> {

    private Deque<Tag> tagDeque = new LinkedList<>();
    private Queue<ByteBuffer> buffers = new LinkedList<>();
    private Queue<ByteBuffer> currentMessage = new LinkedList<>();

    private boolean completeTag = false;

    private long accum = 0;

    private int sizeToLimitMessage = 0;

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();

    private StringBuilder current = new StringBuilder();

    @Override
    public Tag fromByteBuffer(ByteBuffer buffer) {
        Tag tag = null;

//        buffer.limit(buffer.limit() -1); // TODO SACARLO DEL SOCKET CONNECTION HANDLER

        if (parser.getInputFeeder().needMoreInput()) {
            try {
                parser.getInputFeeder().feedInput(buffer);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        } else {
            // TODO:
        }
//        System.out.println(new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8));
        try {
            tag = parse();
            System.out.println("POS DEL BUFFER: " + parser.getLocationInfo().getEndingByteOffset());
            long position = parser.getLocationInfo().getEndingByteOffset() - accum;
            accum = parser.getLocationInfo().getEndingByteOffset();
            System.out.println("POSITION DEL AUX: " + position);

            ByteBuffer aux = buffer.duplicate();

            System.out.println(aux);
            System.out.println(buffer);

            aux.position(buffer.position());
            aux.limit((int) position + aux.position());

            buffer.position((int) position + buffer.position());

            // TODO: APPEND

            System.out.println(aux);
            System.out.println(buffer);

            //TODO ver como mierda calcular mejor el tema del tama;o para delimitar

        } catch (XMLStreamException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("Mal formado");
        }
        return tag;
    }

    private Tag parse() throws XMLStreamException {

        while (parser.hasNext()) {
            switch (parser.next()) {
                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("START DOCUMENT");
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:

                    System.out.println("START ELEMENT");

                    String name = parser.getName().getLocalPart();
                    System.out.println("Name: " + name);

                    if (parser.getPrefix().equals("stream") && name.equals("stream")) {
                        Stream retStream = new Stream();
                        fillTag(retStream);
                        return retStream;
                    }

                    if (tagDeque.isEmpty()) {

                        if (name.equals("stream")) {
                            Stream stream = new Stream();
                            fillTag(stream);
                            completeTag = true;
                            tagDeque.push(stream);

                        } else if (name.equals("auth")) {
                            Auth auth = new Auth();
                            fillTag(auth);
                            completeTag = true;
                            tagDeque.push(auth);
                        } else if (name.equals("message")) {
                            Message m = new Message();
                            fillTag(m);
                            completeTag = true;
                            tagDeque.push(m);
                        } else {
                            //TODO: ver el xml version
                            Tag t = new Tag(name);
                            System.out.println("EL PREFIJO ES " + t.getPrefix());
                            tagDeque.push(t);
                        }

                    } else { //tagDequeue not empty

                        if (completeTag) {
                            Tag t = new Tag(name);
                            fillTag(t);
                            tagDeque.peek().addTag(t);
                            tagDeque.push(t);
                        }
                    }

                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.println("AALO1");
                    System.out.println("CHARACTERS: " + parser.getText());

                    if (completeTag) {
                        tagDeque.peek().setValue(parser.getText());
                    }

                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("END_ELEMENT");
                    System.out.println("en end elemnt la posicion del parser es " + parser.getLocation().getCharacterOffset());

                    ////esto choto
                    sizeToLimitMessage += parser.getName().getLocalPart().toString().length();
                    System.out.println("tama;o de local part " + parser.getName().getLocalPart().toString().length());
                    System.out.println("tama;no del prefijo " + parser.getPrefix().length());

                    sizeToLimitMessage += parser.getPrefix().length();
                    sizeToLimitMessage += 3;
                    if (!parser.getPrefix().toString().equals(""))
                        sizeToLimitMessage += 1;
                    /////

                    System.out.println("sieto limit " + sizeToLimitMessage);


                    if (completeTag) {

                        if (tagDeque.size() == 1) {
                            return tagDeque.poll();
                        } else {
                            tagDeque.poll();
                        }

                    } else {
                        String nameOfEndElement = parser.getName().getLocalPart().toString();
                        if (tagDeque.peek().getName().toString().equals(nameOfEndElement)) {
                            if (tagDeque.peek().getPrefix().toString().equals("") || tagDeque.peek().getPrefix().equals(parser.getPrefix())) {
                                return tagDeque.poll();
                            }

                        }

                    }
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("EVENT INCOMPLETE");
                    return null;

                default:
                    System.out.println("AALOASD");
                    break;


            }

        }

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {
        // TODO: !!!

        return null;
    }

    private int sizeBuffers(Queue<ByteBuffer> bufferList) {
        int i = 0;
        for (ByteBuffer b : bufferList) {
            i += b.remaining();
        }
        return i;
    }

    private void fillTag(Tag tag) {
        addAttributes(tag);
        tag.setPrefix(parser.getPrefix());
        tag.addNamespace(parser.getName().getNamespaceURI());
    }

    private void addAttributes(Tag tag) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            tag.addAttribute(parser.getAttributeName(i).toString(), parser.getAttributeValue(i));
        }
    }
}
