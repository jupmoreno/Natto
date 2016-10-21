package ar.edu.itba.pdc.natto.proxy;

import java.io.IOException;

public interface Server {
    void start() throws IOException;

    void stop();

    boolean isRunning();
}
