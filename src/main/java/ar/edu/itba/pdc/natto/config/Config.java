package ar.edu.itba.pdc.natto.config;

import ar.edu.itba.pdc.natto.net.NetAddress;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.util.*;

public class Config {
    private final FileBasedConfigurationBuilder<XMLConfiguration> builder;
    private final XMLConfiguration config;

    private int nttpPort;
    private final Map<String, String> nttpUsers;

    private int xmppPort;
    private boolean xmppSilenceEnabled;
    private final Set<String> xmppSilencedUsers;
    private boolean xmppTransformationEnabled;
    private NetAddress xmppDefaultServer;
    private final Map<String, NetAddress> xmppUserServers;

    public Config(String path) throws ConfigurationException {
        Configurations configs = new Configurations();

        builder = configs.xmlBuilder(path);
        config = builder.getConfiguration();

        nttpPort = config.getInt("psp.port", Defaults.PSP_PORT);
        if (!NetAddress.isValidPort(nttpPort)) {
            nttpPort = Defaults.PSP_PORT;
        }

        nttpUsers = new HashMap<>();
        List<HierarchicalConfiguration<ImmutableNode>> nttpUsersPasswords =
                config.configurationsAt("psp.passwords.password");
        for (HierarchicalConfiguration nttpUserPassword : nttpUsersPasswords) {
            String user = nttpUserPassword.getString("[@user]", null);

            if (user != null) {
                String password = nttpUserPassword.getString(".", null);

                if (password != null) {
                    nttpUsers.put(user, password);
                }
            }
        }
        if (nttpUsers.isEmpty()) {
            nttpUsers.put("admin", "admin");
        }

        xmppPort = config.getInt("xmpp.port", Defaults.XMPP_PORT);
        if (!NetAddress.isValidPort(xmppPort)) {
            xmppPort = Defaults.XMPP_PORT;
        }

        xmppSilenceEnabled = config.getBoolean("xmpp.silenced[@enabled]", Defaults.SILENCE_ENABLED);

        List<String> silencedUsers = config.getList(String.class, "xmpp.silenced.user[@name]",
                null);
        xmppSilencedUsers = new HashSet<>();
        if (silencedUsers != null) {
            for (String user : silencedUsers) {
                xmppSilencedUsers.add(user.toLowerCase());
            }
        }

        xmppTransformationEnabled = config.getBoolean("xmpp.transformation[@enabled]",
                Defaults.TRANSFORMATION_ENABLED);

        String defaultAddress = config.getString("xmpp.servers.default.address",
                Defaults.SERVER_ADDRESS);
        int defaultPort = config.getInt("xmpp.servers.default.port", Defaults.SERVER_PORT);
        try {
            xmppDefaultServer = new NetAddress(defaultAddress, defaultPort);
        } catch (Exception exception) {
            xmppDefaultServer = new NetAddress(Defaults.SERVER_ADDRESS, Defaults.SERVER_PORT);
        }

        xmppUserServers = new HashMap<>();
        List<HierarchicalConfiguration<ImmutableNode>> serversUsers =
                config.configurationsAt("xmpp.servers.user");
        for (HierarchicalConfiguration server : serversUsers) {
            String name = server.getString("[@name]", null);

            if (name != null) {
                String address = server.getString("address", xmppDefaultServer.getAddress());
                int port = server.getInt("port", xmppDefaultServer.getPort());

                try {
                    NetAddress netAddress = new NetAddress(address, port);
                    xmppUserServers.put(name, netAddress);
                } catch (Exception exception) {
                    // Intentionally
                }
            }
        }
    }

    public void setNttpPort(int nttpPort) {
        if (NetAddress.isValidPort(nttpPort)) {
            this.nttpPort = nttpPort;
        }
    }

    public int getNttpPort() {
        return nttpPort;
    }

    public void setXmppPort(int xmppPort) {
        if (NetAddress.isValidPort(xmppPort)) {
            this.xmppPort = xmppPort;
        }
    }

    public int getXmppPort() {
        return xmppPort;
    }

    public void setXmppSilenceEnabled(boolean xmppSilenceEnabled) {
        this.config.setProperty("xmpp.silenced[@enabled]", xmppSilenceEnabled);
        this.xmppSilenceEnabled = xmppSilenceEnabled;
    }

    public boolean isXmppSilenceEnabled() {
        return xmppSilenceEnabled;
    }

    public Set<String> getXmppSilencedUsers() {
        return xmppSilencedUsers;
    }

    public void setXmppTransformationEnabled(boolean xmppTransformationEnabled) {
        this.config.setProperty("xmpp.transformation[@enabled]", xmppTransformationEnabled);
        this.xmppTransformationEnabled = xmppTransformationEnabled;
    }

    public boolean isXmppTransformationEnabled() {
        return xmppTransformationEnabled;
    }

    public void setXmppDefaultServer(String hostname, int port) {
        this.xmppDefaultServer = new NetAddress(hostname, port);
    }

    public NetAddress getXmppDefaultServer() {
        return xmppDefaultServer;
    }

    public Map<String, NetAddress> getXmppUserServers() {
        return xmppUserServers;
    }

    public Map<String, String> getNttpUsers() {
        return nttpUsers;
    }

    public void save() throws ConfigurationException {
        config.clearTree("psp.passwords");
        for (String user : nttpUsers.keySet()) {
            String password = nttpUsers.get(user);

            config.addProperty("psp.passwords.password(-1)", password);
            config.addProperty("psp.passwords.password[@user]", user);
        }

        config.clearTree("xmpp.silenced");
        config.addProperty("xmpp.silenced[@enabled]", xmppSilenceEnabled);

        for (String user : xmppSilencedUsers) {
            config.addProperty("xmpp.silenced.user(-1)[@name]", user);
        }

        config.clearTree("xmpp.servers");
        config.addProperty("xmpp.servers.default.address", xmppDefaultServer.getAddress());
        config.addProperty("xmpp.servers.default.ip", xmppDefaultServer.getPort());

        for (String key : xmppUserServers.keySet()) {
            NetAddress netAddress = xmppUserServers.get(key);

            config.addProperty("xmpp.servers.user(-1)[@name]", key);
            config.addProperty("xmpp.servers.user.address", netAddress.getAddress());
            config.addProperty("xmpp.servers.user.port", netAddress.getPort());
        }

        builder.save();
    }
}
