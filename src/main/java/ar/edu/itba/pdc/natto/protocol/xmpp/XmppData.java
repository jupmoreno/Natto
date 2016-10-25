package ar.edu.itba.pdc.natto.protocol.xmpp;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.config.Defaults;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

public class XmppData {
    private int port = Defaults.XMPP_PORT;
    private InetSocketAddress defaultAddress = new InetSocketAddress(Defaults.SERVER_ADDRESS,
            Defaults.SERVER_PORT);
    private final Map<String, InetSocketAddress> usersAddress;
    private boolean silence = Defaults.SILENCE_ENABLED;
    private final Set<String> usersSilenced;
    private boolean transform = Defaults.TRANSFORMATION_ENABLED;

    public XmppData(Map<String, InetSocketAddress> usersAddress, Set<String> usersSilenced) {
        this.usersAddress = usersAddress;
        this.usersSilenced = usersSilenced;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDefaultAddress(InetSocketAddress defaultAddress) {
        this.defaultAddress = checkNotNull(defaultAddress);
    }

    public InetSocketAddress setUserAddress(String user, InetSocketAddress address) {
        return usersAddress.put(checkNotNull(user), checkNotNull(address));
    }

    public InetSocketAddress getUserAddress(String user) {
        InetSocketAddress address = usersAddress.get(user);

        if (address == null) {
            address = defaultAddress;
        }

        return address;
    }

    public boolean isUserSilenced(String name) {
        if (!silence) {
            return false;
        }

        return usersSilenced.contains(name);
    }

    public void silenceUser(String name) {
        usersSilenced.add(checkNotNull(name));
    }

    public void setSilence(boolean enabled) {
        this.silence = enabled;
    }

    public void setTransformation(boolean enabled) {
        this.transform = enabled;
    }

    public boolean isTransformEnabled() {
        return transform;
    }
}
