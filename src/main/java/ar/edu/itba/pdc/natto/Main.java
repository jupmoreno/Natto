package ar.edu.itba.pdc.natto;

import ar.edu.itba.pdc.natto.dispatcher.ConcreteDispatcher;
import ar.edu.itba.pdc.natto.dispatcher.Dispatcher;
import ar.edu.itba.pdc.natto.protocol.Parser;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.string.StringParserFactory;
import ar.edu.itba.pdc.natto.protocol.string.StringProtocolFactory;
import ar.edu.itba.pdc.natto.proxy.MultiProtocolServer;
import ar.edu.itba.pdc.natto.proxy.Server;
import ar.edu.itba.pdc.natto.proxy.config.ServerConfig;
import org.aeonbits.owner.ConfigFactory;
import org.kohsuke.args4j.CmdLineException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Arguments arguments = getArguments(args);
        if (arguments == null) {
            return;
        }

        ServerConfig config = getConfig(arguments);
        if (config == null) {
            return;
        }

        startServer(config);
    }

    private static void startServer(ServerConfig config) {
        ParserFactory<String> xmppParsers = new StringParserFactory();
        ProtocolFactory<String> xmppProtocols = new StringProtocolFactory();

        try (Dispatcher dispatcher = new ConcreteDispatcher()) {
            Server proxyServer = new MultiProtocolServer.Builder(dispatcher)
                    .addProtocol(config.getXmppPort(), xmppParsers, xmppProtocols)
                    .build();

            try {
                proxyServer.start();
            } catch (IOException exception) {
                System.err.println("Failed to start Proxy Server");
                System.err.println(exception.getMessage());
                return;
            }
        } catch (IOException exception) {
            System.err.println("Failed to create necessary parts for Proxy Server to operate");
            System.err.println(exception.getMessage());
            return;
        }
    }

    private static Arguments getArguments(String[] args) {
        Arguments arguments = new Arguments();

        try {
            arguments.set(args);
        } catch (CmdLineException exception) {
            // Wrong arguments
            System.err.println(exception.getMessage());
            arguments.printUsage(System.err);
            return null;
        }

        return arguments;
    }

    private static ServerConfig getConfig(Arguments arguments) {
        ServerConfig config;

        if (arguments.getConfigFile() != null) {
            Properties props = new Properties();

            try {
                props.load(new FileInputStream(arguments.getConfigFile()));
            } catch (IOException exception) {
                System.err.println("Invalid file: " + exception.getMessage());
                return null;
            }

            config = ConfigFactory.create(ServerConfig.class, props);
        } else {
            config = ConfigFactory.create(ServerConfig.class);
        }

        if (arguments.getProxyPspPort() != null) {
            config.setPspPort(arguments.getProxyPspPort());
        }

        if (arguments.getProxyXmppPort() != null) {
            config.setXmppPort(arguments.getProxyXmppPort());
        }

        if (arguments.getServerAddress() != null) {
            config.setDefaultServer(arguments.getServerAddress());
        }

        if (arguments.getMaxThreads() != null) {
            config.setMaxThreads(arguments.getMaxThreads());
        }

        System.out.println(config);

        return config;
    }
}
