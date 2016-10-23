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

/*
    private enum XmppState {
        STREAM("stream");

        private final String value;

        XmppState(String value) {
            this.value = value;
        }
    }*/

    private Queue<StringBuilder> parsed = new LinkedList<>();
    private StringBuilder current;
   // private Deque<XmppState> stateStack = new LinkedList<>();
    private Queue<String> tags = new LinkedList<>();
    private String currentTag = "";
    boolean insideTag = false;
    boolean unfinishedTag = false;

    @Override
    public String fromByteBuffer(ByteBuffer buffer) {
        //appendBuffer(buffer);
        String bufferString = bufferToString(buffer);

        /*for (int i = 0; i < bufferString.length(); i++) {
            if (bufferString.charAt(i) == '<') {
                if (true) {

                }

            } else if (bufferString.charAt(i) == '>') {

            }
        }*/
        tagsToQueue(bufferString);

        if(tags.isEmpty())
            return null;
        return tags.remove();
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

    /**
     * Recognizes tags within the string and inserts them into the tags queue.
     *
     * @param bufferString
     */
    void tagsToQueue(String bufferString){

        for(int i = 0; i < bufferString.length(); i++){

            if(!insideTag){

                if(bufferString.charAt(i) == '<'){

                    //info que no pertenece a un tag. Ej: <etiqueta>Soy una etiqueta</etiqueta>
                    if(!currentTag.equals("")){
                        tags.add(currentTag);
                        currentTag = "";
                    }

                    currentTag += bufferString.charAt(i);
                    insideTag = true;
                    unfinishedTag = true;
                }else{
                    currentTag += bufferString.charAt(i);
                    unfinishedTag = true;
                }

            }else{

                if(bufferString.charAt(i) == '>'){
                    currentTag += bufferString.charAt(i);
                    insideTag = false;
                    unfinishedTag = false;
                    tags.add(currentTag);
                    currentTag = "";
                }else{
                    currentTag += bufferString.charAt(i);
                }

            }

        }

    }
}
