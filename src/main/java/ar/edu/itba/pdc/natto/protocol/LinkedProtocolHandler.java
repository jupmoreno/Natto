package ar.edu.itba.pdc.natto.protocol;

import java.nio.ByteBuffer;

public interface LinkedProtocolHandler {
    void link(final LinkedProtocolHandler link);

    void requestRead();

    void requestWrite(final ByteBuffer buffer);

    void finishedWriting();

    void requestClose();
}
