package cotton.servicediscovery;

import java.io.Serializable;
import java.net.SocketAddress;
/**
 * 
 * 
 * @author Magnus
 */
public class AnnouncePacket implements Serializable {
    private SocketAddress instanceAddress;
    private String[] serviceList;

    public AnnouncePacket(SocketAddress instanceAddress, String[] serviceList) {
        this.instanceAddress = instanceAddress;
        this.serviceList = serviceList;
    }

    public SocketAddress getInstanceAddress() {
        return instanceAddress;
    }

    public String[] getServiceList() {
        return serviceList;
    }
    
}
