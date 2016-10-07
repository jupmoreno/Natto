package ar.edu.itba.pdc.tpe.proxy.handlers;

import java.io.IOException;

public interface Handler {
    void handle(int readyOps) throws IOException;
}
