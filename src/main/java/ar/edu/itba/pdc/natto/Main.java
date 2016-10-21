package ar.edu.itba.pdc.natto;

import ar.edu.itba.pdc.natto.dispatcher.ConcreteDispatcher;
import ar.edu.itba.pdc.natto.dispatcher.Dispatcher;
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

        Server proxyServer;
        try {
            Dispatcher dispatcher = new ConcreteDispatcher();
            proxyServer = new MultiProtocolServer.Builder(dispatcher)
                    .addProtocol(arguments.getProxyXMPPPort(), null, null)
                    .build();
        } catch (IOException exception) {
            // TODO:
            System.err.println("Failed to create Proxy Server");
            System.err.println(exception.getMessage());
            return;
        }

        try {
            proxyServer.start();
        } catch (IOException exception) {
            System.err.println("Failed to start Proxy Server");
            System.err.println(exception.getMessage());
            return;
        }
    }
}
