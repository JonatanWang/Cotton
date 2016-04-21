
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
    private DiscoveryPacketType type;

    public DiscoveryPacketType  getPacketType() {
        return type;
    }

    public void setPackettype(DiscoveryPacketType type){
        this.type = type;
    }

    public DiscoveryPacket(DiscoveryPacketType type) {
        this.type = type;
    }
    
    private DiscoveryProbe probe = null;

    public DiscoveryProbe getProbe() {
        return probe;
    }

    public void setProbe(DiscoveryProbe probe) {
        this.probe = probe;
    }
    
}
