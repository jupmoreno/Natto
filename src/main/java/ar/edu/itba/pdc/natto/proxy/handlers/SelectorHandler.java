package ar.edu.itba.pdc.natto.proxy.handlers;

import java.io.IOException;

public interface SelectorHandler {
    void handle(final int readyOps) throws IOException;
}
