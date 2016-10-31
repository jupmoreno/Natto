package ar.edu.itba.pdc.natto.protocol.xmpp.models;



/**
 * The <presence/> stanza is a specialized "broadcast" or "publish-
 subscribe" mechanism, whereby multiple entities receive information
 (in this case, network availability information) about an entity to
 which they have subscribed.
 */
@Deprecated
public class Presence extends Tag {

    public Presence(){
        super("presence");
    }

    @Override
    public boolean isPresence(){
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
     * The 'type' attribute specifies the purpose or context of the presence stanza.
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

}

