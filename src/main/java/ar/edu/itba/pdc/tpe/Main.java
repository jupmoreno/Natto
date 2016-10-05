package ar.edu.itba.pdc.tpe;

import org.kohsuke.args4j.CmdLineException;

import java.net.InetSocketAddress;

public class Main {
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
	}
}
