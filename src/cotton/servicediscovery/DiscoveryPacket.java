
package cotton.servicediscovery;

import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public class DiscoveryPacket implements Serializable {
    public enum DiscoveryPacketType {
        DISCOVERYPROBE,ANNOUNCE
    }

    public DiscoveryPacket(DiscoveryPacketType type) {
    }
    
    private DiscoveryProbe probe = null;

    public DiscoveryProbe getProbe() {
        return probe;
    }

    public void setProbe(DiscoveryProbe probe) {
        this.probe = probe;
    }
    
}
