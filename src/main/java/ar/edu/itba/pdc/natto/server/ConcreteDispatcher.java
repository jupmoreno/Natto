package ar.edu.itba.pdc.natto.server;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.server.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.*;
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
        } else { // TODO: Sacar else?
            if (key.isValid() && key.isConnectable()) {
                ConnectionHandler handler = (ConnectionHandler) key.attachment();
                handler.handle_connect();
            }

            // TODO: Else?
            if (key.isValid() && key.isReadable()) {
                ConnectionHandler handler = (ConnectionHandler) key.attachment();
                handler.handle_read();
            }

            // TODO: Else?
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
    public void subscribe(SelectableChannel channel, int op, SelectorHandler handler)
            throws ClosedChannelException {
        checkNotNull(handler, "Handler can't be null");

        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        if (key != null) {
            channel.register(selector, key.interestOps() | op, handler);
        } else {
            channel.register(selector, op, handler);
        }
    }

    @Override
    public void unsubscribe(SelectableChannel channel, int op) {
        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        if (key != null) {
            key.interestOps(key.interestOps() & ~op); // TODO: Check
        }
    }

    @Override
    public void unsubscribe(SelectableChannel channel) {
        SelectionKey key = checkNotNull(channel, "Channel can't be null").keyFor(selector);

        if (key != null) {
            key.cancel(); // TODO: Check && OPs oreadas?
        }
    }
}
