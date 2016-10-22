package ar.edu.itba.pdc.natto.dispatcher;

import java.nio.channels.SelectionKey;

public enum ChannelOperation {
    ACCEPT(SelectionKey.OP_ACCEPT),
    CONNECT(SelectionKey.OP_CONNECT),
    READ(SelectionKey.OP_READ),
    WRITE(SelectionKey.OP_WRITE),
    READWRITE(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

    private final int value;

    ChannelOperation(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
