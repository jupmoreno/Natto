package ar.edu.itba.pdc.natto.protocol;

public interface Protocol<Message> {
    Message process(Message message);
}
