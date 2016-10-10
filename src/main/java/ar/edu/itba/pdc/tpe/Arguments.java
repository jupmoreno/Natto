package ar.edu.itba.pdc.tpe;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.PrintStream;

public class Arguments {
    @Argument(metaVar = "<serverAddress>", usage = "Sets the server's address", index = 0)
    private String serverAddress = "localhost";

    @Option(name = "-p", aliases = {"--port"}, metaVar = "<port>", usage = "Sets the server's "
            + "port number")
    private int serverPort = 5222;

    @Option(name = "-l", aliases = {"--listen"}, metaVar = "<port>", usage = "Sets the "
            + "listening port number")
    private int listenPort = 1080;

    private CmdLineParser parser;

    public Arguments() {
        parser = new CmdLineParser(this);
    }

    public void set(String[] args) throws CmdLineException {
        parser.parseArgument(args);
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void printUsage(final PrintStream stream) {
        stream.println("Usage:");
        parser.printSingleLineUsage(stream);
        stream.println("\nList of arguments:");
        parser.printUsage(stream);
    }
}
