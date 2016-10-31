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
        super("message");
    }

    @Override
    public boolean isMessage(){
        return true;
    }


    /**
     * The 'id' attribute is used by the originating entity to track any
     response or error stanza that it might receive in relation to the
     generated stanza from another entity (such as an intermediate server
     or the intended recipient).
     */
    public StringBuilder getId(){
        return this.getAttribute("id");
    }

    /**
     * The 'to' attribute specifies the JID of the intended recipient for
     the stanza.
     */
    public StringBuilder getTo(){
        return getAttribute("to");
    }

    /**
     * The 'from' attribute specifies the JID of the sender.
     */
    public StringBuilder getFrom(){
        return getAttribute("from");
    }

    /**
     * The 'type' attribute specifies the purpose or context of the message stanza.
     */
    public StringBuilder getType(){
        return getAttribute("type");
    }

    /**
     * A stanza SHOULD possess an 'xml:lang' attribute (as defined in
     Section 2.12 of [XML]) if the stanza contains XML character data that
     is intended to be presented to a human user
     */
    public StringBuilder getLang(){
        return getAttribute("xml:lang");
    }

    /**
     *
     * @return the messages body, if the message does not have a body it returns null
     */
    public StringBuilder getBody(){
        return getTagContent("body");
    }


}
