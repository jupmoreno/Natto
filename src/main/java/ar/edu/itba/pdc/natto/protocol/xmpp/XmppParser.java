package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class XmppParser implements Parser<String> {

/*
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
*/

    private String current = "";
    private Deque<String> stateStack = new LinkedList<>();
    private Queue<String> tags = new LinkedList<>();
    private String currentTag = "";
    boolean insideTag = false;
    boolean unfinishedTag = false;
    boolean isFile = false;

    /**
     * Returns only complete stanzas, if with what it receives in buffer it cannot finish it then it saves it in current and returns null
     * @param buffer
     * @return
     */
    @Override
    public String fromByteBuffer(ByteBuffer buffer) {

        String bufferString = bufferToString(buffer);
        tagsToQueue(bufferString);

        while(!tags.isEmpty()){

            String tag = tags.remove();
            if(tag.startsWith("<") && !ignoreTag(tag)) {
                System.out.println("SOY EL TAG: " + tagType(tag));
                if (tag.startsWith("</")) {
                    if (!stateStack.peek().equals(tagType(tag))) {
                        //TODO esta mal formado
                    } else {
                        stateStack.pop();
                    }
                }else{
                    if(tagType(tag).equals("file")){
                        isFile = true;
                    }

                    if(!tag.endsWith("/>")){
                        stateStack.push(tagType(tag));
                    }
                }
            }
            current += tag;
        }


        // Estoy enviando un archivo
        if(isFile){
            String ret = current;
            current = "";
            if((!unfinishedTag) && current != "" && stateStack.isEmpty()) {
                isFile = false;
            }
            return ret;
        }

        // Estoy enviando un mensaje completo
        if(current != "" && stateStack.isEmpty()){
            String ret = current;
            current = "";
            return ret;
        }

        // El mensaje todavía no está completo
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
        //bufferString = bufferString.substring(0,bufferString.length()-1);
        //TODO: validar comillas
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

    /**
     * example: <iq to='bar'> will return iq
     *
     * @param tag
     * @return
     */
    private String tagType(String tag){
        String ret = "";
        for(int i=1; i<tag.length(); i++){
            if(tag.charAt(i) == ' ' || tag.charAt(i) == '>'){
                return ret;
            }
            if(tag.charAt(i) != '/')
                ret += tag.charAt(i);
        }
        return ret;
    }

    /**
     * Indicates if a tag has to be ignored, if this is the case then the message will be considered complete even if a closing tag of these type isn't found.
     *
     * @param tag
     * @return
     */
    private boolean ignoreTag(String tag){
        return (tagType(tag).equals("stream:stream") || tagType(tag).startsWith("?xml")) ? true : false;
    }

}
