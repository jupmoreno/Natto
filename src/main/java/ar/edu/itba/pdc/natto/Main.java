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


        try (Dispatcher dispatcher = new ConcreteDispatcher()) {
            Server proxyServer = new MultiProtocolServer.Builder(dispatcher)
                    // TODO: Add factories
                    .addProtocol(arguments.getProxyXMPPPort(), null, null)
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
