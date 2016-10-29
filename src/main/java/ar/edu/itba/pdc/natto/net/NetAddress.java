package ar.edu.itba.pdc.natto.net;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public class NetAddress {
    private final String host;
    private final int port;

    public NetAddress(String host, int port) {
        checkArgument(!Strings.isNullOrEmpty(host), "Invalid hostname");
        checkArgument(isValidPort(port), "Invalid port number");

        this.host = host;
        this.port = port;
    }

    public String getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }
}
