package ar.edu.itba.pdc.natto.protocol.xmpp;

public enum XmppMessages {
    // TODO: No hay q poner mas cosas en el stream?
    INITIAL_STREAM("<stream:stream version='1.0' xml:lang='en' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'>"),
    INITIAL_STREAM_START("<stream:stream version='1.0' xml:lang='en' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' "),
    INITIAL_STREAM_END(">"),
    END_STREAM("</stream:stream>"),
    VERSION_AND_ENCODING("<?xml version='1.0' encoding='UTF-8'?>"),
    STREAM_FEATURES("<stream:features>"
            + "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>PLAIN</mechanism></mechanisms>"
            + "<compression xmlns='http://jabber.org/features/compress'><method>zlib</method></compression>" // TODO: Preguntarle a Diego si tiene q tenerlo
            + "<auth xmlns='http://jabber.org/features/iq-auth'/>"
            + "</stream:features>"),
    ;

    private final byte[] bytes;

    XmppMessages(String message) {
        this.bytes = message.getBytes();
    }

    public byte[] getBytes() {
        return bytes;
    }
}
