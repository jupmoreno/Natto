package ar.edu.itba.pdc.natto.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.net.InetSocketAddress;
import java.util.*;

public class Config {
    private final FileBasedConfigurationBuilder<XMLConfiguration> builder;
    private final XMLConfiguration config;

    // TODO: MaxThreads (?

    private int pspPort;

    private int xmppPort;
    private boolean xmppSilenceEnabled;
    private Set<String> xmppSilencedUsers;
    private boolean xmppTransformationEnabled;
    private InetSocketAddress xmppDefaultServer;
    private Map<String, InetSocketAddress> xmppUserServers;

    public Config(String path) throws ConfigurationException {
        Configurations configs = new Configurations();

        builder = configs.xmlBuilder(path);
        config = builder.getConfiguration();

        pspPort = config.getInt("psp.port", Defaults.PSP_PORT);

        xmppPort = config.getInt("xmpp.port", Defaults.XMPP_PORT);
        xmppSilenceEnabled = config.getBoolean("xmpp.silenced[@enabled]", Defaults.SILENCE_ENABLED);
        List<String> silencedUsers = config.getList(String.class, "xmpp.silenced.user[@name]");
        xmppSilencedUsers = new HashSet<>();
        if (silencedUsers != null) {
            xmppSilencedUsers.addAll(silencedUsers);
        }
        xmppTransformationEnabled = config.getBoolean("xmpp.transformation[@enabled]",
                Defaults.TRANSFORMATION_ENABLED);
        xmppDefaultServer = new InetSocketAddress(
                config.getString("xmpp.servers.default.address", Defaults.SERVER_ADDRESS),
                config.getInt("xmpp.servers.default.port", Defaults.SERVER_PORT));
        xmppUserServers = new HashMap<>();

        List<HierarchicalConfiguration<ImmutableNode>> serversUsers =
                config.configurationsAt("xmpp.servers.user");
        for (HierarchicalConfiguration server : serversUsers) {
            String name = server.getString("[@name]", null);

            if (name != null) {
                String address = server.getString("address", xmppDefaultServer.getHostName());
                int port = server.getInt("port", xmppDefaultServer.getPort());


                xmppUserServers.put(name, new InetSocketAddress(address, port));
            }
        }
    }

    public void setPspPort(int pspPort) {
//        this.config.setProperty("psp.port", pspPort);
        this.pspPort = pspPort;
    }

    public int getPspPort() {
        return pspPort;
    }

    public void setXmppPort(int xmppPort) {
//        this.config.setProperty("xmpp.port", pspPort);
        this.xmppPort = xmppPort;
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
        this.xmppDefaultServer = new InetSocketAddress(checkNotNull(hostname), port);
    }

    public InetSocketAddress getXmppDefaultServer() {
        return xmppDefaultServer;
    }

    public Map<String, InetSocketAddress> getXmppUserServers() {
        return xmppUserServers;
    }

    public void save() throws ConfigurationException {
        config.clearTree("xmpp.silenced");
        config.addProperty("xmpp.silenced[@enabled]", xmppSilenceEnabled);

        for (String user : xmppSilencedUsers) {
            config.addProperty("xmpp.silenced.user(-1)[@name]", user);
        }

        config.clearTree("xmpp.servers");
        config.addProperty("xmpp.servers.default.address", xmppDefaultServer.getHostName());
        config.addProperty("xmpp.servers.default.ip", xmppDefaultServer.getPort());

        for (String key : xmppUserServers.keySet()) {
            InetSocketAddress inet = xmppUserServers.get(key);

            config.addProperty("xmpp.servers.user(-1)[@name]", key);
            config.addProperty("xmpp.servers.user.address", inet.getHostName());
            config.addProperty("xmpp.servers.user.port", inet.getPort());
        }

        builder.save();
    }
}
