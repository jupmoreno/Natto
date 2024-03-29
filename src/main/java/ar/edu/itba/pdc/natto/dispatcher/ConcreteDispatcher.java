package ar.edu.itba.pdc.natto.dispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.proxy.handlers.AcceptHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.ConnectionHandler;
import ar.edu.itba.pdc.natto.proxy.handlers.SelectorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class ConcreteDispatcher implements Dispatcher, DispatcherSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(ConcreteDispatcher.class);

    private final Selector selector;

    public ConcreteDispatcher() throws IOException {
        this.selector = Selector.open();
    }

    @Override
    public void handle_events() throws IOException {
        if (selector.select() != 0) { // TODO: Timeout (?
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove(); // http://stackoverflow.com/q/7132057/3349531

                dispatch(key);
            }
        }
    }

    private void dispatch(SelectionKey key) throws IOException {
        if (key.isValid() && key.isAcceptable()) {
            AcceptHandler handler = (AcceptHandler) key.attachment();
            handler.handle_accept();
        } else {
            if (key.isValid() && key.isConnectable()) {
                ConnectionHandler handler = (ConnectionHandler) key.attachment();
                handler.handle_connect();
            }

            if (key.isValid() && key.isReadable()) {
                ConnectionHandler handler = (ConnectionHandler) key.attachment();
                handler.handle_read();
            }

            if (key.isValid() && key.isWritable()) {
                ConnectionHandler handler = (ConnectionHandler) key.attachment();
                handler.handle_write();
            }
        }
    }

    @Override
    public DispatcherSubscriber getSubscriber() {
        return this;
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

    @Override
    public void subscribe(SelectableChannel channel, ChannelOperation op, SelectorHandler handler) {
        checkNotNull(handler, "Handler can't be null");

        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        try {
            if (key != null) {
                channel.register(selector, key.interestOps() | op.getValue(), handler);
            } else {
                channel.register(selector, op.getValue(), handler);
            }
        } catch (ClosedChannelException exception) {
            logger.error("Requested subscription of closed channel", exception);
            throw new IllegalArgumentException("Channel closed");
        }

    }

    @Override
    public void unsubscribe(SelectableChannel channel, ChannelOperation op) {
        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        if (key != null) {
            key.interestOps(key.interestOps() & ~op.getValue());
        }
    }

    @Override
    public void cancel(SelectableChannel channel) {
        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        if (key != null) {
            key.cancel(); // TODO: Check && OPs oreadas?
        }
    }
}
