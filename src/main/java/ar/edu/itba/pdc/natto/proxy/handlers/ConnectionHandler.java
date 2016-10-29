package ar.edu.itba.pdc.natto.proxy.handlers;

public interface ConnectionHandler extends SelectorHandler {
    void handle_connect();

    void handle_read();

    void handle_write();
}
