package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.*;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

// TODO: Fijarse de siempre cerrar bien el parser anterior!
public class XmppParser implements Parser<Tag> {
    private final static int BUFFER_MAX_SIZE = 10000;
    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;
    private Queue<ByteBuffer> buffers = new LinkedList<>();
    private Deque<Tag> tagDequeue = new LinkedList<>();
    private int type; // TODO: El primer caso pq es 0?
    private boolean completeTag = false;

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
        System.out.println("\nMe llega: " + new String(buffer.array(), buffer.position(), buffer.limit()));

        System.out.println("\n\n\n\n");
        if (buffer == null) {
            return null;
        }
        //buffer.limit(buffer.limit() - 1);

        if (newTag) {
            parser = inputF.createAsyncForByteBuffer();
            newTag = false;
        }

        if (sizeBuffers() + buffer.remaining() > BUFFER_MAX_SIZE) {
            // TODO: Falta cerrar bien el parser
            return handleTooBig();
        }



        if(parser.getInputFeeder().needMoreInput()){
            System.out.println("Necesito mas cosas para el parser");
            try {
                parser.getInputFeeder().feedInput(buffer);

            } catch (XMLStreamException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                newTag = true;
                return null; // TODO: ? Wrongformat tag // Falta cerrar bien el parser //Forwardear o que hacer? ???
            }
        }else{
            System.out.println("el parser tiene cosas, ver que hacer con buffer que entra"); //TODO
        }


        // Aca empieza la etapa de parseo
        Tag message = null;

        int position = 0;
        try {
            message = parse();
            System.out.println("Llegue 1");
            position = (int) parser.getLocationInfo().getEndingByteOffset() + 1;
            System.out.println("Llegue 2");

        } catch (XMLStreamException e) {
            System.out.println(e.getMessage());
            System.out.println("Mensaje mal formado"); // TODO: Loguear
            message = handleWrongFormat();
        }

        ByteBuffer auxBuffer = buffer.slice();
        buffer.position(position + buffer.position());
        System.out.println("Llegue 3");
        auxBuffer.limit(position);
        System.out.println(auxBuffer);
        System.out.println(buffer);
        System.out.println("Llegue 4");

        // Agrego el buffer para devolverlo en el caso de que no lo modifique
        buffers.add(auxBuffer);
        System.out.println("Llegue 5");

        if (message != null) {
            System.out.println(message);

            newTag = true;

            try {
                parser.close();
            } catch (XMLStreamException e) {
                e.printStackTrace(); // TODO: Loguear
            }

            checkState(tagDequeue.isEmpty());
        }

        completeTag = false;

        return message;
    }

    private Tag parse() throws XMLStreamException {
        Tag ret = null;


        while (parser.hasNext()) {
            Tag tag;

            switch (parser.next()) {
                case AsyncXMLStreamReader.START_ELEMENT:
                    System.out.println("start element " + parser.getName());

                    if (tagDequeue.isEmpty()) {
                        String name = parser.getName().getLocalPart().toString();

                        if (name.equals("iq")) {
                            tag = new Iq();
                        } else if (name.equals("presence")) {
                            tag = new Presence();
                        } else if (name.equals("message")) {
                            completeTag = true;
                            tag = new Message();
                        } else if (name.equals("stream")) {
                            tag = new Stream();

                        } else if (name.equals("auth")){
                            completeTag = true;
                            tag = new Tag("auth", false); //TODO: crear un objeto auth
                        } else {
                            tag = new Tag(name, false);
                        }
                        addAttributes(tag);
                        tag.setPrefix(parser.getPrefix());
                        tag.addNamespace(parser.getName().getNamespaceURI());

                        if(parser.getPrefix().equals("stream")){
                            // addAttributes(tag);
                            // tag.setPrefix(parser.getPrefix());
                            // tag.addNamespace(parser.getName().getNamespaceURI());
                            System.out.println("tamaño -> " + parser.getLocationInfo().getEndingByteOffset());
                            parser.getInputFeeder().endOfInput();
                            return tag;
                        }else{
                            tagDequeue.push(tag);
                        }

                    } else {
                        if(completeTag){
                            boolean empty = true;

                            try {
                                empty = parser.isEmptyElement();
                            } catch (XMLStreamException e) {
                                // TODO: Que hacer?
                                e.printStackTrace();
                                // TODO: Return?
                            }

                            tag = new Tag(parser.getName().getLocalPart(), empty);
                            addAttributes(tag);
                            tag.setPrefix(parser.getPrefix());
                            tag.addNamespace(parser.getName().getNamespaceURI());
                            tagDequeue.peek().addTag(tag);
                            tagDequeue.push(tag);
                        }else{
                            tagDequeue.push(new Tag(parser.getName().getLocalPart(), false));
                        }

                    }
                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.println("Character: " + parser.getText());

                    if(completeTag){
                        tagDequeue.peek().setValue(parser.getText());
                    }

                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("End element: " + parser.getName());
                    System.out.println("tama;o de la pila " + tagDequeue.size());

                        if (tagDequeue.size() == 1) {
                           parser.getInputFeeder().endOfInput();
                           // System.out.println("CIERRO EL INPUT ACA EN EL END_ELEMENT");
                           // newTag = true;
                            ret = tagDequeue.poll();
                        } else {
                            tagDequeue.poll();
                        }

                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("Incomplete! En el stack tengo: " + tagDequeue.peek());
                    if (tagDequeue.isEmpty()){
                        parser.getInputFeeder().endOfInput();
                        return ret;
                    }
                    return null;

                default:
                    // Ignore
                    break;
            }
        }
        System.out.println("Tamaño" + parser.getLocationInfo().getEndingByteOffset());
        checkNotNull(ret);

        return ret;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {
        if ((!message.isModified()) || message.isWrongFormat()) {
            ByteBuffer ret = ByteBuffer.allocate(sizeBuffers());
            while (!buffers.isEmpty()) {
                ret.put(buffers.poll());
            }

            ret.flip();
            System.out.println("Devuelvo: " + new String(ret.array(), ret.position(), ret.limit(),
                    StandardCharsets.UTF_8));
            return ret;
        }
        while (!buffers.isEmpty()) {
            //TODO: hasta donde borrar?
            buffers.poll();
        }
        System.out.println("aca estoy justo antes de devolver algo modificado");
        System.out.println("Devulelvo: " + message);
        return ByteBuffer.wrap(message.toString().getBytes());
    }

    private void addAttributes(Tag tag) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            tag.addAttribute(parser.getAttributeName(i).toString(), parser.getAttributeValue(i));
        }
    }

    private Tag handleWrongFormat() {
        Tag tag;

        while (tagDequeue.size() > 1) {
            tagDequeue.poll();
        }

        if (tagDequeue.size() == 1) {
            tag = tagDequeue.poll();
        } else {
            tag = new Tag("", true);
        }

        tag.setWrongFormat(true);

        return tag;
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
