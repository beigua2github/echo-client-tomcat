package com.spring.app.mvc;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class Utils {
    private static final String NETWORK_CARD = "eth0|en0|";

    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces();
            while (e1.hasMoreElements()) {
                NetworkInterface ni = e1.nextElement();
                if (NETWORK_CARD.indexOf(ni.getName() + "|") >= 0) {
                    Enumeration<InetAddress> e2 = ni.getInetAddresses();
                    while (e2.hasMoreElements()) {
                        InetAddress ia = e2.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;
                        }
                        return ia.getHostAddress();
                    }
                    break;
                } else {
                    continue;
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
