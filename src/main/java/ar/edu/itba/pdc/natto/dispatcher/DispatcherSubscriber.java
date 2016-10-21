package ar.edu.itba.pdc.natto.dispatcher;

import ar.edu.itba.pdc.natto.proxy.handlers.SelectorHandler;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;

public interface DispatcherSubscriber {
    void subscribe(SelectableChannel channel, int op, SelectorHandler connector)
            throws ClosedChannelException;

//    void subscribe(SelectableChannel channel, int op) throws ClosedChannelException; // TODO:

    void unsubscribe(SelectableChannel channel, int op);

    void cancel(SelectableChannel channel);
}
