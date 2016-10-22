package ar.edu.itba.pdc.natto.proxy;

import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;

// TODO: No hace falta, sacarla?
public class SocketInformation<T> {
    private final int port;
    private final ProtocolFactory<T> protocols;
    private final ParserFactory<T> parsers;

    public SocketInformation(final int port, final ParserFactory<T> parsers,
                             final ProtocolFactory<T> protocols) {
        this.port = port;
        this.parsers = parsers;
        this.protocols = protocols;
    }

    public int getPort() {
        return port;
    }

    public ProtocolFactory<T> getProtocols() {
        return protocols;
    }

    public ParserFactory<T> getParsers() {
        return parsers;
    }
}
