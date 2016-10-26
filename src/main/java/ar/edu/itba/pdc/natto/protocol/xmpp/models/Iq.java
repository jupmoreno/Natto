package ar.edu.itba.pdc.natto.protocol.xmpp.models;

import java.util.Map;

/**
 * Created by natinavas on 10/25/16.
 */
public class Iq extends Tag {

    public Iq(){
        super("iq", false);
    }

    @Override
    public boolean isIq(){
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
        return this.getAttribute("type");
    }

    public StringBuilder getLang(){
        return getAttribute("xml:lang");
    }
}
