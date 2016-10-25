package ar.edu.itba.pdc.natto.config;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.PrintStream;

public class Arguments {
    @Argument(metaVar = "<address>", usage = "Sets the default XMPP server hostname", index = 0)
    private String serverAddress;

    @Argument(metaVar = "<port>", usage = "Sets the default XMPP server port", index = 1)
    private Integer serverPort;

    @Option(name = "--xmpp-port", metaVar = "<port>", usage = "Sets the proxy's XMPP listening"
            + " port number")
    private Integer proxyXmppPort;

    @Option(name = "--psp-port", metaVar = "<port>", usage = "Sets the proxy's PSP listening"
            + " port number")
    private Integer proxyPspPort;

    @Option(name = "--config-file", aliases = {"-c"}, metaVar = "<path>",
            usage = "Sets the proxy's config file")
    private String configPath = Defaults.CONFIG_PATH;

    // TODO: MaxThreads (?
//    @Option(name = "--max-threads", metaVar = "<number>", usage = "Sets the proxy's max. threads")
//    private Integer maxThreads;

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

    public Integer getServerPort() {
        return serverPort;
    }

    public Integer getProxyXmppPort() {
        return proxyXmppPort;
    }

    public Integer getProxyPspPort() {
        return proxyPspPort;
    }

    public String getConfigPath() {
        return configPath;
    }

//    public Integer getMaxThreads() {
//        return maxThreads;
//    }
}
