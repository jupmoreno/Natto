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

    public StringBuilder getFrom(){
        return this.getAttribute("from");
    }

    public StringBuilder getType(){
        return this.getAttribute("type");
    }
}
