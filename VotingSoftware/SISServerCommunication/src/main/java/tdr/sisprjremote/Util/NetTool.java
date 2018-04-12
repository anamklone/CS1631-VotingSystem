package tdr.sisprjremote.Util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetTool {
    /**
     * If an address can be used for public access
     *
     * @param inetAddress an address
     * @return public or not
     */
    private static boolean isPublic(InetAddress inetAddress) {
        return !inetAddress.isAnyLocalAddress()
                && !inetAddress.isLinkLocalAddress()
                && !inetAddress.isLoopbackAddress()
                && !inetAddress.isMCGlobal() && !inetAddress.isMCLinkLocal()
                && !inetAddress.isMCNodeLocal() && !inetAddress.isMCOrgLocal()
                && !inetAddress.isMCSiteLocal()
                && !inetAddress.isMulticastAddress();
    }

    /**
     * Get the public address of this machine
     *
     * @return public address
     */
    public static String getPublicAddress() {
        Enumeration<NetworkInterface> e;
        try {
            e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                Enumeration<InetAddress> addrs = e.nextElement()
                        .getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress inetAddress = addrs.nextElement();
                    if (isPublic(inetAddress)
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e1) {
            // System.out.println(e1.getMessage());
        }

        return null;
    }
}