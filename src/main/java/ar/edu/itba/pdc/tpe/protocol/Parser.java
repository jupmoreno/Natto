package ar.edu.itba.pdc.tpe.protocol;

public interface Parser<MessageType> {
    MessageType parse(final byte[] data);
}
