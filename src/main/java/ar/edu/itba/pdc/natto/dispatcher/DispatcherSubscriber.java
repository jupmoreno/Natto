package ar.edu.itba.pdc.natto.dispatcher;

import ar.edu.itba.pdc.natto.proxy.handlers.SelectorHandler;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;

public interface DispatcherSubscriber {
    void subscribe(SelectableChannel channel, ChannelOperation op, SelectorHandler connector);

//    void subscribe(SelectableChannel channel, int op); // TODO:

    void unsubscribe(SelectableChannel channel, ChannelOperation op);

    void cancel(SelectableChannel channel);
}
