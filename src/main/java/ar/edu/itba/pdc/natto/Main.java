package ar.edu.itba.pdc.natto;

import ar.edu.itba.pdc.natto.proxy.ProxyServer;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.net.InetSocketAddress;

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

        InetSocketAddress serverAddress = new InetSocketAddress(arguments.getServerAddress(),
                arguments.getServerPort());
        ProxyServer proxyServer = new ProxyServer(serverAddress, arguments.getProxyXMPPPort(),
                arguments.getProxyPSPPort());

        try {
            proxyServer.start();
        } catch (IOException exception) {
            System.err.println("Failed to start Proxy Server");
            System.err.println(exception.getMessage());
            return;
        }
    }
}
