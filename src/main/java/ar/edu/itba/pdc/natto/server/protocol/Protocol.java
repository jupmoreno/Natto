package ar.edu.itba.pdc.natto.server.protocol;

public interface Protocol<Message> {
    Message process(Message message);
}
