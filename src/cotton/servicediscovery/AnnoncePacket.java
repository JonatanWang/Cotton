
package cotton.servicediscovery;
import java.io.Serializable;
import java.net.SocketAddress;
/**
 *
 * @author Magnus
 */
public class AnnoncePacket implements Serializable {
    private SocketAddress instanceAddress;
    private String[] serviceList;

    public AnnoncePacket(SocketAddress instanceAddress, String[] serviceList) {
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
