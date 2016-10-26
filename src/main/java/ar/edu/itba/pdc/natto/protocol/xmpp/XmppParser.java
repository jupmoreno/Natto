package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Iq;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Message;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Presence;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

public class XmppParser implements Parser<Tag> {

    AsyncXMLInputFactory inputF = new InputFactoryImpl();
    String message = "<iqqq:iq xmlns:iqqq=\"holaaaaaa\"><hola></hola></iqqq:iq>";
    ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());
    AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;

    Deque<Tag> tagQueue = new LinkedList<>();


    public XmppParser() {
        try {
            parser = inputF.createAsyncFor(buffer2);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Tag fromByteBuffer(ByteBuffer buffer) {

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
                        } else {
                            System.out.println("TEINE OTRO NOMBRE");
                        }

                    } else {
                        boolean empty = true;
                        try {
                            empty = parser.isEmptyElement();
                        } catch (XMLStreamException e) {
                            e.printStackTrace();
                        }

                        tag = new Tag(parser.getName().getLocalPart(), empty);
                        System.out.println("name " + parser.getName().getLocalPart());
                        tagQueue.peek().addTag(tag);

                    }
                    addAttributes(tag);
                    System.out.println("prefix que le asigno al tag " + parser.getPrefix());
                    System.out.println("namespace uri que le asigno al tag " + parser.getName().getNamespaceURI());
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

        return ByteBuffer.wrap(message.toString().getBytes());
    }

    private void addAttributes(Tag tag) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            tag.addAttribute(parser.getAttributeName(i).toString(), parser.getAttributeValue(i));

        }
    }

}
