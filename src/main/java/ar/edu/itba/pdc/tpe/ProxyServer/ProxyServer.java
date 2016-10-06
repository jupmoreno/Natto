package ar.edu.itba.pdc.tpe.ProxyServer;

import ar.edu.itba.pdc.tpe.ProxyServer.handlers.AcceptHandler;
import ar.edu.itba.pdc.tpe.ProxyServer.handlers.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
	private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
	
	private final InetSocketAddress serverAddress;
	private final int port;
	
	private final ExecutorService executors;
	
	public ProxyServer (final InetSocketAddress serverAddress, final int port) {
		this.serverAddress = serverAddress;
		this.port = port;
		
		executors = Executors.newCachedThreadPool(); // TODO: Aca? O en run()?
	}
	
	public void run() throws IOException {
		try (
				Selector selector = Selector.open();
				ServerSocketChannel channel = ServerSocketChannel.open();
		) {
			ServerSocket socket = channel.socket();
			
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler(selector, channel, serverAddress));
			socket.bind(new InetSocketAddress(port));
			
			handleConnections(selector);
		} catch (IOException e) {
			logger.error("Couldn't start ProxyServer", e);
			// TODO:
			throw e;
		}
	}
	
	public void handleConnections (final Selector selector) {
		try {
			while (true) {
				if(selector.select() != 0) {
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove(); // http://stackoverflow.com/q/7132057/3349531
						
						dispatch(key);
					}
				}
			}
		} catch (IOException e) {
			logger.error("Server closed", e);
			// TODO:
		}
	}
	
	private void dispatch(final SelectionKey key) {
		if(!key.isValid()) {
			return;
		}
		
		Handler handler = (Handler) key.attachment();
		
		try {
			handler.handle();
		} catch(IOException e) {
			logger.error("Handling error", e);
			// TODO:
		}
	}
}
