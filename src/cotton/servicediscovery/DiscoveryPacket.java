
package cotton.servicediscovery;

import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public class DiscoveryPacket implements Serializable {
    public enum DiscoveryPacketType {
        DISCOVERYREQUEST, DISCOVERYRESPONSE, ANNOUNCE
    }
    private DiscoveryPacketType type;

    public DiscoveryPacket(DiscoveryPacketType type) {
        this.type = type;
    }
    
    public DiscoveryPacketType getPacketType() {
        return type;
    }

    public void setPacketType(DiscoveryPacketType type) {
        this.type = type;
    }
    
    private DiscoveryProbe probe = null;
    private AnnouncePacket annonce = null;

    public AnnouncePacket getAnnonce() {
        return annonce;
    }

    public void setAnnonce(AnnouncePacket annonce) {
        this.annonce = annonce;
    }
    
    public DiscoveryProbe getProbe() {
        return probe;
    }

    public void setProbe(DiscoveryProbe probe) {
        this.probe = probe;
    }
    
}
