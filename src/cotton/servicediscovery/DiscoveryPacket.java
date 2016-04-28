package cotton.servicediscovery;

import java.io.Serializable;

/**
 * The <code>DiscoveryPacket</code> acts as a wrapper for the <code>DiscoveryProbe</code>
 * and the <code>AnnouncePacket</code>. Each packet consists of a <code>DiscoveryPacketType</code>
 * used to determine the packet purpose.
 * 
 * @author Mats
 * @author Magnus
 * @see DiscoveryProbe
 * @see AnnouncePacket
 */
public class DiscoveryPacket implements Serializable {
    private DiscoveryProbe probe = null;
    private AnnouncePacket announce = null;
    private DiscoveryPacketType type;
    
    public enum DiscoveryPacketType {
        DISCOVERYREQUEST, DISCOVERYRESPONSE, ANNOUNCE
    }

    /**
     * Constructs an empty <code>DiscoveryPacket</code> consisting only of the 
     * <code>DiscoveryPacketType</code>.
     * 
     * @param type the packet type.
     */
    public DiscoveryPacket(DiscoveryPacketType type) {
        this.type = type;
    }
    
    /**
     * Returns the purpose of the <code>DiscoveryPacket</code> represented as a
     * <code>DiscoveryPacketType</code>.
     * 
     * @return the <code>DiscoveryPacketType</code>.
     */
    public DiscoveryPacketType getPacketType() {
        return type;
    }

    /**
     * Changes the <code>DiscoveryPacketType</code> through the parameter.
     * 
     * @param type the new <code>DiscoveryPacketType</code>.
     */
    public void setPacketType(DiscoveryPacketType type) {
        this.type = type;
    }

    /**
     * Returns the <code>AnnouncePacket</code> connected to the <code>DiscoveryPacket</code>.
     * 
     * @return the connected <code>AnnouncePacket</code>.
     */
    public AnnouncePacket getAnnounce() {
        return announce;
    }

    /**
     * Sets the containing <code>AnnouncePacket</code> to the incoming packet.
     * 
     * @param announce new <code>AnnouncePacket</code>.
     */
    public void setAnnonce(AnnouncePacket announce) {
        this.announce = announce;
    }
    
    /**
     * Returns the <code>DiscoveryProbe</code> connected to the <code>DiscoveryPacket</code>.
     * 
     * @return the connected <code>DiscoveryProbe</code>.
     */
    public DiscoveryProbe getProbe() {
        return probe;
    }

     /**
     * Sets the containing <code>DiscoveryProbe</code> to the incoming probe.
     * 
     * @param probe new <code>AnnounceProbe</code>.
     */
    public void setProbe(DiscoveryProbe probe) {
        this.probe = probe;
    }    
}
