
package cotton.network;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Random;

import cotton.services.ServiceConnection;
import java.util.UUID;

/**
 *
 * @author Magnus
 */
public class DefaultServiceConnection implements ServiceConnection {
    private UUID conId;
    private String name;
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
    
}
