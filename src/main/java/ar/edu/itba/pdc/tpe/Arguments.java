package ar.edu.itba.pdc.tpe;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.PrintStream;

public class Arguments {
	@Argument(metaVar = "<server_address>", usage = "Sets the server's address", index = 0/*, required = true*/)
	private String server_address = "localhost";
	@Option(name = "-p", aliases = {"--port"}, metaVar = "<port>", usage = "Sets the server's port number")
	private int server_port = 5222;
	@Option(name = "-l", aliases = {"--listen"}, metaVar = "<port>", usage = "Sets the listening port number")
	private int listen_port = 1080;
	private CmdLineParser parser;
	
	public Arguments () {
		parser = new CmdLineParser(this);
	}
	
	public void set (String[] args) throws CmdLineException {
		parser.parseArgument(args);
	}
	
	public String getServerAddress () {
		return server_address;
	}
	
	public int getServerPort () {
		return server_port;
	}
	
	public int getListenPort () {
		return listen_port;
	}
	
	public void printUsage (PrintStream out) {
		out.println("Usage:");
		parser.printSingleLineUsage(out);
		out.println("\nList of arguments:");
		parser.printUsage(out);
	}
}
