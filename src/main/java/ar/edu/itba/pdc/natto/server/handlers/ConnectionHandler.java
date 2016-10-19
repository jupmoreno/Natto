package ar.edu.itba.pdc.natto.server.handlers;

import java.io.IOException;

public interface ConnectionHandler extends SelectorHandler {
    void handle_connect() throws IOException;

    void handle_read() throws IOException;

    void handle_write() throws IOException;
}
