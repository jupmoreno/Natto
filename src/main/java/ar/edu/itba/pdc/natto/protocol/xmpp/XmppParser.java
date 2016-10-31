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

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();



    @Override
    public Tag fromByteBuffer(ByteBuffer buffer) {
        return null;
    }

    private Tag parse() throws XMLStreamException {

        while(parser.hasNext()){

            switch (parser.next()){
                case AsyncXMLStreamReader.START_DOCUMENT:
                    System.out.println("START DOCUMENT");
                    break;

                case AsyncXMLStreamReader.START_ELEMENT:
                    System.out.println("START ELEMENT");

                    String name = parser.getName().getLocalPart().toString();
                    System.out.println("Name: " + name);

                    if(parser.getPrefix().equals("stream") && name.equals("stream")){
                        Stream retStream = new Stream();
                        fillTag(retStream);
                        return retStream;
                    }

                    if(tagDeque.isEmpty()){

                        if(name.equals("stream")){
                            Stream stream = new Stream();
                            fillTag(stream);
                            completeTag = true;
                            tagDeque.push(stream);

                        }else if(name.equals("auth")){
                            Auth auth = new Auth();
                            fillTag(auth);
                            completeTag = true;
                            tagDeque.push(auth);
                        }else if(name.equals("message")){
                            Message m = new Message();
                            fillTag(m);
                            completeTag = true;
                            tagDeque.push(m);
                        }else{
                            //TODO: ver el xml version
                            Tag t = new Tag(name);
                            tagDeque.push(t);
                        }

                    }else{ //tagDequeue not empty

                        if(completeTag){
                            Tag t = new Tag(name);
                            fillTag(t);
                            tagDeque.peek().addTag(t);
                            tagDeque.push(t);
                        }
                    }

                    break;

                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.println("CHARACTERS");

                    if(completeTag){
                        tagDeque.peek().setValue(parser.getText());
                    }

                    break;

                case AsyncXMLStreamReader.END_ELEMENT:
                    System.out.println("END_ELEMENT");

                    if(completeTag){

                        if(tagDeque.size() == 1){
                            return tagDeque.poll();
                        }else{
                            tagDeque.poll();
                        }

                    }else{
                        String nameOfEndElement = parser.getName().getLocalPart().toString();
                        System.out.println("Name: " + nameOfEndElement);

                        if(tagDeque.peek().getName().equals(nameOfEndElement) && tagDeque.peek().getPrefix().equals(parser.getPrefix())){
                            return tagDeque.poll();
                        }

                    }
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return null;

                default:
                    break;


            }

        }

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Tag message) {
        return null;
    }

    private void fillTag(Tag tag){
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
