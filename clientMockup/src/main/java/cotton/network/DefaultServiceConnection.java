package cotton.network;

import java.net.SocketAddress;
import java.io.Serializable;

import java.util.UUID;

import cotton.network.PathType;
import cotton.network.ServiceConnection;

/**
 *
 * @author Magnus
 */
public class DefaultServiceConnection implements ServiceConnection, Serializable {
    private static final long serialVersionUID = 1L;
    private UUID conId;
    private String name;
    private PathType pathType = PathType.SERVICE;
    SocketAddress address = null;

    public DefaultServiceConnection() {
        conId = UUID.randomUUID();
        this.name = "none";
    }

    public DefaultServiceConnection(UUID uuid) {
        conId = uuid;
        this.name = "none";
    }

    public DefaultServiceConnection(String name) {
        conId = UUID.randomUUID();
        this.name = name;
    }

    @Override
    public UUID getUserConnectionId() {
        return this.conId;
    }

    @Override
    public String getServiceName() {
        return this.name;
    }

    @Override
    public SocketAddress getAddress() {
        return this.address;
    }

    @Override
    public void setAddress(SocketAddress addr) {
        this.address = addr;
    }

    @Override
    public PathType getPathType() {
        return this.pathType;
    }

    @Override
    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }
    
}
