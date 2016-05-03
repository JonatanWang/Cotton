package cotton.servicediscovery;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * The <code>AnnouncePacket</code> wraps the service list and <code>SocketAddress</code> 
 * of a <code>Cotton</code> instance.
 * 
 * @author Magnus
 * @see SocketAddress
 */
public class QueuePacket implements Serializable {
    private SocketAddress instanceAddress;
    private String[] requestQueueList;

    /**
     * Constructs a <code>AnnouncePacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     * 
     * @param instanceAddress the <code>Cotton</code> instance address.
     * @param serviceList the <code>Cotton</code> instance service list.
     */
    public QueuePacket(SocketAddress instanceAddress, String[] serviceList) {
        this.instanceAddress = instanceAddress;
        this.requestQueueList = requestQueueList;
    }

    /**
     * Returns the containing <code>SocketAddress</code> of the 
     * <code>AnnouncePacket</code>.
     * 
     * @return the containing <code>SocketAddress</code>.
     */
    public SocketAddress getInstanceAddress() {
        return instanceAddress;
    }

    /**
     * Returns the containing <code>serviceList</code> of the 
     * <code>AnnouncePacket</code>.
     * 
     * @return the containing <code>serviceList</code>.
     */
    public String[] getRequestQueueList() {
        return requestQueueList;
    }
}
