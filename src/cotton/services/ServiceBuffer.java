package cotton.services;

/**
 * A <code>ServiceBuffer</code> stores incoming <code>ServicePackets</code> and 
 * distributes packets.
 *
 * @author Tony Tran
 * @see ServicePacket
 **/
public interface ServiceBuffer{

    /**
     * Distributes the next packet in the <ocde>ServiceBuffer</code>.
     * 
     * @return the next <code>ServicePacket</code> in the buffer.
     */
    public ServicePacket nextPacket();
    
    /**
     * Stores a <code>ServicePacket</code> in the buffer.
     * 
     * @param servicePacket the <code>ServicePacket</code> to store.
     * @return <code>true</code> if the buffer changed as a result of the <code>add</code>.
     */
    public boolean add(ServicePacket servicePacket);
}
