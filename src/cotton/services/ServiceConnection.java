package cotton.services;

import java.net.InetAddress;

/**
 *
 *
 * @author Tony
 * @author Magnus
 */
public interface ServiceConnection {
    public String getServiceName();
    public Integer getUserConnectionId();
    public InetAddress getAddress();
    public void setAddress(InetAddress addr);
}
