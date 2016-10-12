package ar.edu.itba.pdc.tpe;

import ar.edu.itba.pdc.tpe.proxy.ProxyServer;
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
        }

        InetSocketAddress serverAddress = new InetSocketAddress(arguments.getServerAddress(),
                arguments.getServerPort());
        if (serverAddress.isUnresolved()) {
            System.err.println("Unresolved server address: " + serverAddress);
            return;
        }

        ProxyServer proxyServer = new ProxyServer(serverAddress, arguments.getListenPort());

        try {
            proxyServer.start();
        } catch (IOException exception) {
            System.err.println("Failed to start Proxy Server");
            //return;
        }
    }
}
