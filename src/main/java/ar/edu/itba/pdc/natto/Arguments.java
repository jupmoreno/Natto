package ar.edu.itba.pdc.natto;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.PrintStream;

public class Arguments {
    @Argument(metaVar = "<host:port>", usage = "Sets the default XMPP server address")
    private String serverAddress;

    @Option(name = "--xmpp-port", metaVar = "<port>", usage = "Sets the proxy's XMPP listening"
            + " port number")
    private String proxyXmppPort;

    @Option(name = "--psp-port", metaVar = "<port>", usage = "Sets the proxy's PSP listening"
            + " port number")
    private String proxyPspPort;

    @Option(name = "--config-file", metaVar = "<path>", usage = "Sets the proxy's config file")
    private File configFile;

    @Option(name = "--max-threads", metaVar = "<number>", usage = "Sets the proxy's max. threads")
    private String maxThreads;

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

    public String getProxyXmppPort() {
        return proxyXmppPort;
    }

    public String getProxyPspPort() {
        return proxyPspPort;
    }

    public File getConfigFile() {
        return configFile;
    }

    public String getMaxThreads() {
        return maxThreads;
    }
}
