package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class XmppParser implements Parser<String> {


    private enum XmppState {
        PRESENCE("presence"),
        IQ("iq"),
        MESSAGE("message"),
        ;

        private final String value;

        XmppState(String value) {
            this.value = value;
        }
    }

    private Queue<StringBuilder> parsed = new LinkedList<>();
    private String current = "";
    private Deque<String> stateStack = new LinkedList<>();
    private Queue<String> tags = new LinkedList<>();
    private String currentTag = "";
    boolean insideTag = false;
    boolean unfinishedTag = false;

    /**
     * Returns only complete stanzas, if with what it receives in buffer it cannot finish it then it saves it in current and returns null
     * @param buffer
     * @return
     */
    @Override
    public String fromByteBuffer(ByteBuffer buffer) {
        //String bufferString = bufferToString(buffer);


        String bufferString = "<stream><iq><holaaaa>hola</holaaaa></iq>";
        tagsToQueue(bufferString);


        for(String tag : tags){
            if(tag.startsWith("<") && !ignoreTag(tag)){
                if(tag.startsWith("</")){
                    if(!stateStack.peek().equals(tagType(tag))){
                        //TODO esta mal formado
                    }else{
                        stateStack.pop();
                    }
                }else{
                    stateStack.push(tagType(tag));
                }
            }
            current += tag;
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

    private String tagType(String tag){
        String ret = "";
        for(int i=1; i<tag.length(); i++){
            if(tag.charAt(i) == ' ' || tag.charAt(i) == '>'){
                return ret;
            }
            if(tag.charAt(i) != '/')
                ret+=tag.charAt(i);
        }
        return ret;
    }

    private boolean ignoreTag(String tag){
        return (tagType(tag).equals("stream") || tagType(tag).startsWith("?xml")) ? true : false;
    }

}
