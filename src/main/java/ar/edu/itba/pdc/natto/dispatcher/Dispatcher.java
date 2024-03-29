package ar.edu.itba.pdc.natto.dispatcher;

import java.io.Closeable;
import java.io.IOException;

public interface Dispatcher extends Closeable, AutoCloseable {
    void handle_events() throws IOException;

    DispatcherSubscriber getSubscriber();
}
