package ar.edu.itba.pdc.natto.proxy.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Mutable;

import java.net.InetSocketAddress;

@Config.LoadPolicy(Config.LoadType.FIRST)
@Config.Sources({"classpath:natto.conf"})
public interface ServerConfig extends Mutable {
    // TODO: Crear clase q la contenga para limitar metodos

    String PSP_PORT = "configuration.proxy.port";
    String XMPP_PORT = "configuration.proxy.xmpp.port";
    String SERVERS_DEFAULT = "configuration.servers.default";
    String MAX_THREADS = "configuration.proxy.threads";

    @Config.Key(PSP_PORT)
    @Config.DefaultValue("1081")
    int getPspPort();

    // TODO: Hacerlo en una clase?
    default String setPspPort(String port) {
        return this.setProperty(PSP_PORT, port);
    }

    @Config.Key(MAX_THREADS)
    @Config.DefaultValue("5")
    int getMaxThreads();

    // TODO: Hacerlo en una clase?
    default String setMaxThreads(String number) {
        return this.setProperty(MAX_THREADS, number);
    }

    @Config.Key(XMPP_PORT)
    @Config.DefaultValue("1080")
    int getXmppPort();

    // TODO: Hacerlo en una clase?
    default String setXmppPort(String port) {
        return this.setProperty(XMPP_PORT, port);
    }

    @Config.Key("configuration.proxy.xmpp.transformation.enabled")
    @Config.DefaultValue("true")
    boolean isTransformationEnabled();

    @Config.Key("configuration.proxy.xmpp.silence.enabled")
    @Config.DefaultValue("true")
    boolean isSilenceEnabled();

    @Config.Key("configuration.proxy.xmpp.silence.users")
    String[] getSilencedUsers();

    @Config.Key(SERVERS_DEFAULT)
    @Config.DefaultValue("localhost:5222")
    @ConverterClass(InetSocketAddressConverter.class)
    InetSocketAddress getDefaultServer();

    // TODO: Hacerlo en una clase?
    default String setDefaultServer(String address) {
        return this.setProperty(SERVERS_DEFAULT, address);
    }

    @Config.Key("configuration.servers.users")
    String[] getUsersServers(); // TODO:
}
