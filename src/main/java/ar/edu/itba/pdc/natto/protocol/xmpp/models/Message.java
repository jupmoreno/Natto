package ar.edu.itba.pdc.natto.protocol.xmpp.models;

/**
 * Created by natinavas on 10/25/16.
 */

/**
 *  The <message/> stanza is a "push" mechanism whereby one entity pushes
 information to another entity, similar to the communications that
 occur in a system such as email.
 */
public class Message extends Tag {

    public Message(){
        super("message", false);
    }

    @Override
    public boolean isMessage(){
        return true;
    }




    public StringBuilder getId(){
        return this.getAttribute("id");
    }

    public StringBuilder getTo(){
        return getAttribute("to");
    }

    public StringBuilder getFrom(){
        return getAttribute("from");
    }

    /**
     * The 'type' attribute specifies the purpose or context of the message,
     presence, or IQ stanza.
     */
    public StringBuilder getType(){
        return getAttribute("type");
    }

    public StringBuilder getLang(){
        return getAttribute("xml:lang");
    }

    public StringBuilder getBody(){
        return getTagContent("body");
    }


}
