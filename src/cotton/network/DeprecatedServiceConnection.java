package cotton.network;

import cotton.network.PathType;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.UUID;

/**
 *
 *
 * @author Tony
 * @author Magnus
 */
@Deprecated
public interface DeprecatedServiceConnection {
    public String getServiceName();
    public UUID getUserConnectionId();
    public SocketAddress getAddress();
    public void setAddress(SocketAddress addr);
    public PathType getPathType();
    public void setPathType(PathType pathType);
}
