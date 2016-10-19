package ar.edu.itba.pdc.natto.protocol;

public interface ProtocolFactory<Message> {
    Protocol<Message> get();
}
