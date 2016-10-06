package ar.edu.itba.pdc.tpe.ProxyServer.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler implements Handler {
	private final Logger logger = LoggerFactory.getLogger(AcceptHandler.class);
	
	private final Selector selector;
	private final ServerSocketChannel channel;
	private final InetSocketAddress serverAddress;
	
	public AcceptHandler(Selector selector, ServerSocketChannel channel, InetSocketAddress serverAddress) {
		this.selector = selector;
		this.channel = channel;
		this.serverAddress = serverAddress;
	}
	
	@Override
	public void handle() throws IOException {
		SocketChannel client = null;
		SocketChannel server = null;
		
		try {
			client = channel.accept();
			
			if (client != null) {
				logger.info("Accepted connection from " + client.socket().getRemoteSocketAddress());
				client.configureBlocking(false);
				
				logger.info("Requesting server connection...");
				server = SocketChannel.open(); // ASK: Aca?
				server.configureBlocking(false);
				
				server.connect(serverAddress);
				server.register(selector, SelectionKey.OP_CONNECT, new ConnectionHandler(selector, client, server));
				selector.wakeup(); // TODO: Sacar? ASK: Hay que hacerlo? Cuando hay q hacerlo?
			}
		} catch (IOException e) {
			logger.error(serverAddress + " couldn't establish connection with client", e);
			// TODO: Close
		}
	}
}

