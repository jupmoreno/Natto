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
        STREAM("stream"),
        PRECENSE("presence"),
        IQ("iq"),
        ;

        private final String value;

        XmppState(String value) {
            this.value = value;
        }
    }

    private Queue<StringBuilder> parsed = new LinkedList<>();
    private String current = "";
    private Deque<XmppState> stateStack = new LinkedList<>();
    private Queue<String> tags = new LinkedList<>();
    private String currentTag = "";
    boolean insideTag = false;
    boolean unfinishedTag = false;

    /**
     * Returns only complete stanzas, if with what it recieves in buffer it cannot finish it then it saves it in current and returns null
     * @param buffer
     * @return
     */
    @Override
    public String fromByteBuffer(ByteBuffer buffer) {
        //String bufferString = bufferToString(buffer);


        String bufferString = "<stream><iq><holaaaaa>hola</holaaaa></iq>";
        tagsToQueue(bufferString);


        for(String tag : tags){
            if(!tag.startsWith("<")){
                current += tag;
            }else if(tag.startsWith("<presence")){
                current += tag;
                stateStack.push(XmppState.PRECENSE);
            }else if(tag.startsWith("</presence")){
                if(stateStack.peek() != XmppState.PRECENSE){
                    //TODO: error
                }else{
                    current+= tag;
                    stateStack.pop();
                }
            }else if(tag.startsWith("<iq")){
                current += tag;
                stateStack.push(XmppState.IQ);
            }else if(tag.startsWith("</iq")){
                if(stateStack.peek() != XmppState.IQ){
                    //TODO: error
                }else{
                    current+= tag;
                    stateStack.pop();
                }
            }else{
                current+= tag;
            }

        }

        if(current != "" && stateStack.isEmpty()){
            String ret = current;
            current = "";
            return ret;
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
