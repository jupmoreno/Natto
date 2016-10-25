package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Iq;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Message;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Presence;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class XmppParser implements Parser<String> {

    AsyncXMLInputFactory inputF = new InputFactoryImpl();
    String message = "<iq><mierda>chau</mierda></iq>";
    ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());
    AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;

    Deque<Tag> tagQueue = new LinkedList<>();




    @Override
    public String fromByteBuffer(ByteBuffer buffer) {

        int type = 0;

        try {
            parser = inputF.createAsyncFor(buffer2);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        Tag stanza = null;
        int i = 0;
        do{
           // System.out.println(++i);
            Tag tag = null;
            switch (type) {
                case XMLEvent.START_DOCUMENT:
                    System.out.println("start document");
                    break;
                case XMLEvent.START_ELEMENT:
                    if(tagQueue.size() == 0){
                        if(parser.getName().toString().equals("iq")){
                            tag = new Iq();
                        }else if(parser.getName().toString().equals("presence")){
                            tag = new Presence();
                        }else if(parser.getName().toString().equals("message")){
                            tag = new Message();
                        }
                        tagQueue.push(tag);
                    }else{
                        boolean empty = true;
                        try {
                            empty = parser.isEmptyElement();
                        } catch (XMLStreamException e) {
                            e.printStackTrace();
                        }
                        tag = new Tag(parser.getName().toString(), empty);
                        tagQueue.peek().addTag(tag);
                        tagQueue.push(tag);

                    }
                  //  System.out.println("start element: " + parser.getName());
                    break;
                case XMLEvent.CHARACTERS:
                    tag = tagQueue.poll();
                    tag.setValue(parser.getText());
                    tagQueue.push(tag);

                  //  System.out.println("characters: " + parser.getText());
                    break;

                case XMLEvent.END_ELEMENT:
                    if(tagQueue.size()== 1){
                        stanza = tagQueue.poll();
                        parser.getInputFeeder().endOfInput();
                    }
                    tagQueue.poll();
                  //  System.out.println("end element: " + parser.getName());
                    break;

                case XMLEvent.END_DOCUMENT:
                   // System.out.println("end document");
                    break;

                default:
                    break;
            }


            try {
                type = parser.next();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }

        }while(type != XMLEvent.END_DOCUMENT);


        System.out.println(stanza);

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(String message) {
        return ByteBuffer.wrap(message.getBytes());
    }


}
