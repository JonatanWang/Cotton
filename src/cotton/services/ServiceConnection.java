package cotton.services;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.UUID;

/**
 *
 *
 * @author Tony
 * @author Magnus
 */
public interface ServiceConnection {
    public String getServiceName();
    public UUID getUserConnectionId();
    public SocketAddress getAddress();
    public void setAddress(SocketAddress addr);
}
