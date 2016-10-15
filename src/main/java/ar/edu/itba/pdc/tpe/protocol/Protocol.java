package ar.edu.itba.pdc.tpe.protocol;

public interface Protocol<MessageType> {
    MessageType process(MessageType message);
}
