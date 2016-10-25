package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
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
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class XmppParser implements Parser<String> {

    AsyncXMLInputFactory inputF = new InputFactoryImpl();
    String message = "<hola><mierda>chau</mie";
    ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());
    AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;

    enum stanzaType {IQ, MESSAGE, PRESENCE, };
    stanzaType currentType;


    @Override
    public String fromByteBuffer(ByteBuffer buffer) {

        int type = 0;

        try {
            parser = inputF.createAsyncFor(buffer2);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        do{

            switch (type) {
                case XMLEvent.START_DOCUMENT:
                    System.out.println("start document");
                    break;
                case XMLEvent.START_ELEMENT:
                    System.out.println("start element: " + parser.getName());
                    break;
                case XMLEvent.CHARACTERS:
                    System.out.println("characters: " + parser.getText());
                    break;
                case XMLEvent.END_ELEMENT:
                    System.out.println("end element: " + parser.getName());
                    break;
                case XMLEvent.END_DOCUMENT:
                    System.out.println("end document");
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



        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(String message) {
        return ByteBuffer.wrap(message.getBytes());
    }


}
