package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.xmpp.models.Tag;

/**
 * Created by user on 26/10/16.
 */
public class XmppProtocol implements Protocol<Tag>{

    @Override
    public Tag process(Tag message) {
        System.out.println(message);
        return message;
    }
}
