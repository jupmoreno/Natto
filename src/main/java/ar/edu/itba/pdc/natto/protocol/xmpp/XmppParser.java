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
    String message = "<hola><mierda>chau</mierda></hola>";
    ByteBuffer buffer2 = ByteBuffer.wrap(message.getBytes());
    AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = null;



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


//// now can access couple of events
//        assertTokenType(XMLStreamConstants.START_DOCUMENT, parser.next());
//        assertTokenType(XMLStreamConstants.START_ELEMENT, parser.next());
//        assertEquals("root", parser.getLocalName());
//// since we have parts of CHARACTERS, we'll still get that first:
//        assertTokenType(XMLStreamConstants.CHARACTERS, parser.next());
//        assertEquals("val", parser.getValue();
//// but that's all data we had so:
//        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, parser.next());
//
//// at this point, must feed more data:
//        byte[] input_part2 = "ue</root>".getBytes("UTF-8");
//        parser.getInputFeeder().feedInput(input_part2);
//
//// and can parse that
//        assertTokenType(XMLStreamConstants.CHARACTERS, parser.next());
//        assertEquals("ue", parser.getValue();
//        assertTokenType(XMLStreamConstants.END_ELEMENT, parser.next());
//        assertEquals("root", parser.getLocalName());
//        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, parser.next());
//
//// and if we now ran out of data need to indicate that too
//        parser.getInputFeeder().endOfInput();
//// which lets us conclude parsing
//        assertTokenType(XMLStreamConstants.END_DOCUMENT, parser.next());
//        parser.close();
//
        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(String message) {
        return ByteBuffer.wrap(message.getBytes());
    }


}
