package ar.edu.itba.pdc.natto.server.protocol;

public interface ProtocolFactory<Message> {
    Protocol<Message> get();
}
