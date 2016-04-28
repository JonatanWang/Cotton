package cotton.servicediscovery;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * The <code>DiscoveryProbe</code> acts as a discovery request as well as a
 * discovery response. The packet consists of a service name and a 
 * <code>SocketAddress</code> containing the targeted service address. 
 * 
 * @author Magnus
 */
public class DiscoveryProbe implements Serializable {
    private String name;
    private SocketAddress address;

    /**
     * Constructs a <code>DiscoveryPack</code> through the in parameters.
     * 
     * @param name the service name.
     * @param address the service address.
     */
    public DiscoveryProbe(String name, SocketAddress address) {
        this.name = name;
        this.address = address;
    }

    /**
     * Returns the service name contained in the packet.
     * 
     * @return the service name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the service name in the packet.
     * 
     * @param name the service name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the <code>SocketAddress</code> contained in the packet. This address
     * points to the service location.
     * 
     * @return the <code>SocketAddress</code>
     */
    public SocketAddress getAddress() {
        return address;
    }

    /**
     * Sets the <code>SocketAddress</code> in the packet. This address should 
     * point to the service location.
     * 
     * @param address the new address.
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }        
}
