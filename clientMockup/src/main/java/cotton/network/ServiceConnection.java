package cotton.network;

import java.net.SocketAddress;
import java.util.UUID;

import cotton.network.PathType;

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
    public PathType getPathType();
    public void setPathType(PathType pathType);
}
