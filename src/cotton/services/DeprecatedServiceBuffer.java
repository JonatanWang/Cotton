package cotton.services;

/**
 * A <code>ServiceBuffer</code> stores incoming <code>ServicePackets</code> and 
 * distributes packets.
 *
 * @author Tony Tran
 * @see DeprecatedServicePacket
 **/
public interface DeprecatedServiceBuffer{

    /**
     * Distributes the next packet in the <code>ServiceBuffer</code>.
     * 
     * @return the next <code>ServicePacket</code> in the buffer.
     */
    public DeprecatedServicePacket nextPacket();
    
    /**
     * Stores a <code>ServicePacket</code> in the buffer.
     * 
     * @param servicePacket the <code>ServicePacket</code> to store.
     * @return <code>true</code> if the buffer changed as a result of the <code>add</code>.
     */
    public boolean add(DeprecatedServicePacket servicePacket);
}