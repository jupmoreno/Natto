package ar.edu.itba.pdc.natto.protocol.xmpp;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by natinavas on 10/25/16.
 */
public class Stanza {

    enum stanzaType {IQ, MESSAGE, PRESENCE, };

    private stanzaType type;

    private Map<String, String> attributes = new HashMap<>();

    private Map<String, String> tag = new HashMap<>();


}
