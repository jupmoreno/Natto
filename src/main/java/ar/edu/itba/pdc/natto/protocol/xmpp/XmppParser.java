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
    Deque<Tag> tagQueue = new LinkedList<>();


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

            while(tagQueue.size() > 1){
                tagQueue.poll();
            }
            if(tagQueue.size() == 1){
                Tag tag = tagQueue.poll();
                tag.setTooBig(true);
                return tag;
            }
            Tag retTag = new Tag("", true);
            retTag.setTooBig(true);


            //TODO revisar
            parser.getInputFeeder().endOfInput();

            return retTag;
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
            switch (type) {

                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("start document");
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    if (tagQueue.size() == 0) {
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
                        tagQueue.peek().addTag(tag);

                    }
                    addAttributes(tag);
                    tag.setPrefix(parser.getPrefix());
                    tag.addNamespace(parser.getName().getNamespaceURI());
                    tagQueue.push(tag);
                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    tag = tagQueue.poll();
                    tag.setValue(parser.getText());
                    tagQueue.push(tag);
                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    if (tagQueue.size() == 1) {
                        stanza = tagQueue.poll();

                        //TODO: Sacar end of input aca (?)
                        parser.getInputFeeder().endOfInput();
                    }
                    tagQueue.poll();
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    System.out.println("incomplete");
                    return stanza;

                default:
                    break;
            }

            try {
                type = parser.next();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }

        } while (type != AsyncXMLStreamReader.END_DOCUMENT);



        System.out.println(stanza);

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {

        parser.getInputFeeder().endOfInput();


        if(!message.isModified()){
            ByteBuffer ret = ByteBuffer.allocate(sizeBuffers());
            while(!buffers.isEmpty()){
                ret.put(buffers.poll());
            }
            return ret;
        }

        return ByteBuffer.wrap(message.toString().getBytes());
    }

    private void addAttributes(Tag tag) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            tag.addAttribute(parser.getAttributeName(i).toString(), parser.getAttributeValue(i));

        }
    }

}
