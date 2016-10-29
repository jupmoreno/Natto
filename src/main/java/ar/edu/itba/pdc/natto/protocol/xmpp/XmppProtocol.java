package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Message;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;

/**
 * Created by user on 26/10/16.
 */
public class XmppProtocol implements Protocol<Tag>{

    @Override
    public Tag process(Tag message) {
        //TODO: ver si el usuario esta silenciado
        System.out.println("El mensaje en el process de protocol es: " + message);

        if(message.isWrongFormat())
            return message;

        if(message.isTooBig()){
            //TODO
            return message;
        }

        if(message.isMessage()){
            //TODO: ver si el usuario tiene habilitado el l33t
            l33tBody((Message) message);
        }

        return message;
    }

    private void l33tBody(Message m){
        StringBuilder body = m.getBody();
        if(body == null)
            return;

        replaceAll(body,"a","4");
        replaceAll(body,"e","3");
        replaceAll(body,"i","1");
        replaceAll(body,"o","0");
        replaceAll(body,"c","\\<");

        m.setModified(true);
    }

    private void replaceAll(StringBuilder builder, String from, String to) {
        int index = builder.indexOf(from);
        while (index != -1)
        {
            builder.replace(index, index + from.length(), to);
            index += to.length(); // Move to the end of the replacement
            index = builder.indexOf(from, index);
        }
    }

}


