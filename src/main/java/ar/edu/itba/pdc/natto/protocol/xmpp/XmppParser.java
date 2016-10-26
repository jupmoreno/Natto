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

public class XmppParser implements Parser<Tag> {

    String message = "<iqqq:iq xmlns:iqqq=\"holaaaaaa\"><hola></hola></iqqq:iq>";
    ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());

    final static int BUFFER_MAX_SIZE = 10000;

    AsyncXMLInputFactory inputF = new InputFactoryImpl();
    AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;
    Queue<ByteBuffer> buffers = new LinkedList<>();
    Deque<Tag> tagDequeue = new LinkedList<>();


    public XmppParser() {
        parser = inputF.createAsyncForByteBuffer();

    }


    private int sizeBuffers(){
        int i =0;
        for(ByteBuffer b: buffers){
            i+= b.remaining();
        }
        return i;
    }

    @Override
    public Tag fromByteBuffer(ByteBuffer buffer) {

        if(buffer == null){
            return null;
        }

        //Verifico que todos los buffers no superen el maximo tama;o de mensaje
        if(sizeBuffers() + buffer.remaining() > BUFFER_MAX_SIZE){
            return handleTooBig();
        }

        //Agrego el buffer para devolverlo en el caso de que no lo modifique
        buffers.add(buffer);


        try {
            parser.getInputFeeder().feedInput(buffer);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        //Aca empieza la etapa de parseo
        int type = 0;

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
                        } else if(parser.getName().getLocalPart().toString().equals("stream")){
                            tag = new Stream();
                            addAttributes(tag);
                            tag.setPrefix(parser.getPrefix());
                            tag.addNamespace(parser.getName().getNamespaceURI());
                            return tag;

                        } else {
                            tag = new Tag(parser.getName().getLocalPart(), false);
                        }

                    } else {
                        boolean empty = true;
                        try {
                            empty = parser.isEmptyElement();
                        } catch (XMLStreamException e) {
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

                        //TODO: Sacar end of input aca (?)
                        //<parser.getInputFeeder().endOfInput();
                    }
                    tagDequeue.poll();
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("incomplete");
                    return stanza;

                default:
                    break;
            }

            try {
                type = parser.next();
            } catch (com.fasterxml.aalto.WFCException e) {
                return handleWrongFormat(tag);
            }catch (XMLStreamException e) {
                e.printStackTrace();
            }

        } while (type != AsyncXMLStreamReader.END_DOCUMENT);



        System.out.println(stanza);

        return stanza;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {

        //parser.getInputFeeder().endOfInput();


        if((!message.isModified()) || message.isWrongFormat()){
            ByteBuffer ret = ByteBuffer.allocate(sizeBuffers());
            while(!buffers.isEmpty()){
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

    private Tag handleWrongFormat(Tag tag){
        while(tagDequeue.size() > 1){
            tagDequeue.poll();
        }
        if(tagDequeue.size() == 1){
            tag = tagDequeue.poll();
            tag.setWrongFormat(true);
            return tag;
        }
        Tag retTag = new Tag("", true);
        retTag.setWrongFormat(true);


        //TODO revisar
        parser.getInputFeeder().endOfInput();

        return retTag;
    }

    private Tag handleTooBig(){
        while(tagDequeue.size() > 1){
            tagDequeue.poll();
        }
        if(tagDequeue.size() == 1){
            Tag tag = tagDequeue.poll();
            tag.setTooBig(true);
            return tag;
        }
        Tag retTag = new Tag("", true);
        retTag.setTooBig(true);


        //TODO revisar
        parser.getInputFeeder().endOfInput();

        return retTag;
    }

}
