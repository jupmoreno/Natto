package ar.edu.itba.pdc.tpe;

import ar.edu.itba.pdc.tpe.ProxyServer.ProxyServer;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
	
	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		
		try {
			arguments.set(args);
		} catch (CmdLineException e) {
			// Wrong arguments
			System.err.println(e.getMessage());
			arguments.printUsage(System.err);
		}
		
		InetSocketAddress serverAddress = new InetSocketAddress(arguments.getServerAddress(), arguments.getServerPort());
		if (serverAddress.isUnresolved()) {
			System.err.println("Unresolved server address: " + serverAddress);
			return;
		}
		
		ProxyServer proxyServer;
		try {
			proxyServer = new ProxyServer(serverAddress, arguments.getListenPort());
		} catch (IOException e) {
			logger.error("Failed to create Proxy Server");
			e.printStackTrace();
			return;
		}
		
		try {
			proxyServer.start();
		} catch (IOException e) {
			logger.error("Failed to start Proxy Server");
			e.printStackTrace();
		}
	}
}
