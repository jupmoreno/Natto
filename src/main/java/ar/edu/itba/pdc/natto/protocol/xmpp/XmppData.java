package ar.edu.itba.pdc.natto.protocol.xmpp;

import static com.google.common.base.Preconditions.checkNotNull;

import ar.edu.itba.pdc.natto.config.Defaults;
import ar.edu.itba.pdc.natto.net.NetAddress;

import java.util.Map;
import java.util.Set;

public class XmppData {
    private int port = Defaults.XMPP_PORT;
    private NetAddress defaultAddress = new NetAddress(Defaults.SERVER_ADDRESS,
            Defaults.SERVER_PORT);
    private final Map<String, NetAddress> usersAddress;
    private boolean silence = Defaults.SILENCE_ENABLED;
    private final Set<String> usersSilenced;
    private boolean transform = Defaults.TRANSFORMATION_ENABLED;

    private long bytesTransferred = 0;
    private long accessesAmount = 0;



    private long acceptedAmount = 0;

    public XmppData(Map<String, NetAddress> usersAddress, Set<String> usersSilenced) {
        this.usersAddress = usersAddress;
        this.usersSilenced = usersSilenced;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDefaultAddress(NetAddress defaultAddress) {
        this.defaultAddress = checkNotNull(defaultAddress);
    }

    public NetAddress setUserAddress(String user, NetAddress address) {
        return usersAddress.put(checkNotNull(user), checkNotNull(address));
    }

    public NetAddress getUserAddress(String user) {
        NetAddress address = usersAddress.get(user);

        if (address == null) {
            address = defaultAddress;
        }

        return address;
    }

    public boolean isUserSilenced(String name) {
        return silence && usersSilenced.contains(name);
    }

    public void silenceUser(String name) {
        usersSilenced.add(checkNotNull(name));
    }

    public void unsilenceUser(String name) {
        usersSilenced.remove(checkNotNull(name));
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

    public Set<String> getUsersSilenced(){
        return usersSilenced;
    }

    public void newAccess(){
        accessesAmount++;
    }

    public void moreBytesTransferred(int bytes){
        bytesTransferred += bytes;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public long getAccessesAmount() {
        return accessesAmount;
    }

    public long getAcceptedAmount() {
        return acceptedAmount;
    }

    public void setAcceptedAmount(long acceptedAmount) {
        this.acceptedAmount = acceptedAmount;
    }

    public void newAccepted(){
        this.acceptedAmount++ ;
    }
}
