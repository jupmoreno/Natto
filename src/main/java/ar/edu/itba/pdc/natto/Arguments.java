package ar.edu.itba.pdc.natto;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.PrintStream;

public class Arguments {
    @Argument(metaVar = "<server_address>", usage = "Sets the XMPP server address", index = 0)
    private String serverAddress = "localhost";

    @Argument(metaVar = "<server_port>", usage = "Sets the XMPP server port number", index = 1)
    private int serverPort = 5222;

    @Option(name = "--xmpp-port", metaVar = "<port>", usage = "Sets the proxy's XMPP listening"
            + " port number")
    private int proxyXMPPPort = 1080;

    @Option(name = "--psp-port", metaVar = "<port>", usage = "Sets the proxy's PSP listening"
            + " port number")
    private int proxyPSPPort = 1081;

    private CmdLineParser parser;

    public Arguments() {
        parser = new CmdLineParser(this);
    }

    public void set(String[] args) throws CmdLineException {
        parser.parseArgument(args);
    }

    public void printUsage(final PrintStream stream) {
        stream.println("Usage:");
        parser.printSingleLineUsage(stream);
        stream.println("\nWhere:");
        parser.printUsage(stream);
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getProxyXMPPPort() {
        return proxyXMPPPort;
    }

    public int getProxyPSPPort() {
        return proxyPSPPort;
    }
}
