package ar.edu.itba.pdc.natto;

import ar.edu.itba.pdc.natto.config.Arguments;
import ar.edu.itba.pdc.natto.config.Config;
import ar.edu.itba.pdc.natto.dispatcher.ConcreteDispatcher;
import ar.edu.itba.pdc.natto.dispatcher.Dispatcher;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppParserFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppProtocolFactory;
import ar.edu.itba.pdc.natto.proxy.MultiProtocolServer;
import ar.edu.itba.pdc.natto.proxy.Server;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {
//        XmppParser parser = new XmppParser();
//        ByteBuffer buffer = ByteBuffer.wrap("<stream:stream xmlns:stream=\"asd.org\"><b>asd</b>".getBytes());
//        while (buffer.hasRemaining()) {
//            System.out.println(parser.fromByteBuffer(buffer));
//        }
//
//        if (true) {
//            return;
//        }

        Arguments arguments = new Arguments();

        try {
            arguments.set(args);
        } catch (CmdLineException exception) {
            System.err.println(exception.getMessage());
            arguments.printUsage(System.err);
            return;
        }

        Config configuration;
        try {
            configuration = new Config(arguments.getConfigPath());
        } catch (ConfigurationException exception) {
            System.err.println("Configuration error: [" + exception.getMessage() + "]");
            return;
        }

        fillConfigWithArguments(configuration, arguments);

        startServer(configuration);

        try {
            configuration.save();
        } catch (ConfigurationException exception) {
            System.err.println("Can't save configuration [" + exception.getMessage() + "]");
            return;
        }
    }

    private static void startServer(Config config) {
        ParserFactory<ByteBuffer> xmppParsers = new XmppParserFactory();
        ProtocolFactory<ByteBuffer> xmppProtocols = new XmppProtocolFactory();

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

    private static void fillConfigWithArguments(Config configuration, Arguments arguments) {
        if (arguments.getProxyPspPort() != null) {
            configuration.setPspPort(arguments.getProxyPspPort());
        }

        if (arguments.getProxyXmppPort() != null) {
            configuration.setXmppPort(arguments.getProxyXmppPort());
        }

        if (arguments.getServerAddress() != null) {
            int port = configuration.getXmppDefaultServer().getPort();

            if (arguments.getServerPort() != null) {
                port = arguments.getServerPort();
            }

            try {
                configuration.setXmppDefaultServer(arguments.getServerAddress(), port);
            } catch (Exception exception) {
                // Intentionally
            }
        }
    }
}
