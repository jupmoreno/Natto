package ar.edu.itba.pdc.natto.protocol.xmpp.models;

/**
 * Created by natinavas on 10/25/16.
 */
public class Presence extends Tag {

    public Presence(){
        super("presence", false);
    }

    @Override
    public boolean isPresence(){
        return true;
    }
}

