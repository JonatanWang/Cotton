package cotton.services;

import java.net.InetAddress;
import java.net.SocketAddress;

/**
 *
 *
 * @author Tony
 * @author Magnus
 */
public interface ServiceConnection {
    public String getServiceName();
    public Integer getUserConnectionId();
    public SocketAddress getAddress();
    public void setAddress(SocketAddress addr);
}
