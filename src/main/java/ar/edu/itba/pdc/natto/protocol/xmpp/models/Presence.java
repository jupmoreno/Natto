package ar.edu.itba.pdc.natto.protocol.xmpp.models;

/**
 * Created by natinavas on 10/25/16.
 */

/**
 * The <presence/> stanza is a specialized "broadcast" or "publish-
 subscribe" mechanism, whereby multiple entities receive information
 (in this case, network availability information) about an entity to
 which they have subscribed.
 */
public class Presence extends Tag {

    public Presence(){
        super("presence", false);
    }

    @Override
    public boolean isPresence(){
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

}

