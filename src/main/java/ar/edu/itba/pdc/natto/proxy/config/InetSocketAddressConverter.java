package ar.edu.itba.pdc.natto.proxy.config;

import org.aeonbits.owner.Converter;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public class InetSocketAddressConverter implements Converter<InetSocketAddress> {
    private static final int DEFAULT_PORT = 5222;
    private static final String SEPARATOR = ":";

    @Override
    public InetSocketAddress convert(Method method, String input) {
        String[] split = input.split(SEPARATOR, 2);
        String hostname = split[0];
        Integer port = DEFAULT_PORT;

        if (split.length == 2) {
            port = Integer.valueOf(split[1]);
        }

        return new InetSocketAddress(hostname, port);
    }
}
