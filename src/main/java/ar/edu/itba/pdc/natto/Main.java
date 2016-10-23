package ar.edu.itba.pdc.natto;

import ar.edu.itba.pdc.natto.dispatcher.ConcreteDispatcher;
import ar.edu.itba.pdc.natto.dispatcher.Dispatcher;
import ar.edu.itba.pdc.natto.protocol.ParserFactory;
import ar.edu.itba.pdc.natto.protocol.ProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.string.StringParserFactory;
import ar.edu.itba.pdc.natto.protocol.string.StringProtocolFactory;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppParser;
import ar.edu.itba.pdc.natto.proxy.MultiProtocolServer;
import ar.edu.itba.pdc.natto.proxy.Server;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Arguments arguments = new Arguments();

        try {
            arguments.set(args);
        } catch (CmdLineException exception) {
            // Wrong arguments
            System.err.println(exception.getMessage());
            arguments.printUsage(System.err);
            return;
        }

        ParserFactory<String> xmppParsers = new StringParserFactory();
        ProtocolFactory<String> xmppProtocols = new StringProtocolFactory();

        XmppParser parser = new XmppParser();
        System.out.println(parser.fromByteBuffer(null));


        try (Dispatcher dispatcher = new ConcreteDispatcher()) {
            Server proxyServer = new MultiProtocolServer.Builder(dispatcher)
                    .addProtocol(arguments.getProxyXmppPort(), xmppParsers, xmppProtocols)
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
}
