package ar.edu.itba.pdc.natto.protocol.nttp;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NttpData {
    private Map<String, String> passwords;

    public NttpData(Map<String, String> passwords) {
        checkNotNull(passwords);
        checkArgument(!passwords.isEmpty());

        this.passwords = passwords;
    }

    public String getPassword(String user) {
        return passwords.get(user);
    }
}
