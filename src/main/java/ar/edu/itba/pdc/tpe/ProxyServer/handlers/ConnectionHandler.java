package ar.edu.itba.pdc.tpe.ProxyServer.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ConnectionHandler implements Handler {
	private final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
	
	private final Selector selector;
	private final SocketChannel client;
	private final SocketChannel server;
	
	public ConnectionHandler(Selector selector, SocketChannel client, SocketChannel server) {
		this.selector = selector;
		this.client = client;
		this.server = server;
	}
	
	@Override
	public void handle() throws IOException {
		try {
			server.finishConnect();
			logger.info("Established connection with server on " + server.socket().getRemoteSocketAddress());
//			server.register(selector, SelectionKey.OP_READ, new IOHandler(selector, server, client)); // TODO: OP?
//			client.register(selector, SelectionKey.OP_READ, new IOHandler(selector, client, server));
			selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando hay q hacerlo?
		} catch (Exception e) {
			logger.error("Couldn't establish connection with server", e);
			// TODO: Close
		}
	}
}
