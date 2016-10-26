package ar.edu.itba.pdc.natto.protocol.xmpp.models;

/**
 * Created by natinavas on 10/25/16.
 */
public class Message extends Tag {

    public Message(){
        super("message", false);
    }

    @Override
    public boolean isMessage(){
        return true;
    }


    public StringBuilder getBody(){
        return this.getTagContent("body");
    }

    public StringBuilder getTo(){
        return this.getAttribute("to");
    }


}
