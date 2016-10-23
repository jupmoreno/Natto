package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class XmppParser implements Parser<String> {


    private enum XmppState {
        STREAM("stream");

        private final String value;

        XmppState(String value) {
            this.value = value;
        }
    }

    private Queue<StringBuilder> parsed = new LinkedList<>();
    private StringBuilder current;
    private Deque<XmppState> stateStack = new LinkedList<>();

    @Override
    public String fromByteBuffer(ByteBuffer buffer) {
        //appendBuffer(buffer);
        String bufferString = bufferToString(buffer);
        for (int i = 0; i < bufferString.length(); i++) {
            if (bufferString.charAt(i) == '<') {
                if (bufferString.) {

                }

            } else if (bufferString.charAt(i) == '>') {

            }
        }

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(String message) {
        return null;
    }

    /**
     * Converts a byte buffer to a string
     *
     * @param buffer
     * @return buffer to string
     */
    private String bufferToString(ByteBuffer buffer) {
        return new String(buffer.array(), buffer.position(), buffer.limit(),
                Charset.forName("UTF-8"));
    }
}
