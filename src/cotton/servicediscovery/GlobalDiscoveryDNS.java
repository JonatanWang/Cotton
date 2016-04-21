
package cotton.servicediscovery;

import java.net.SocketAddress;
/**
 *
 * @author Magnus
 */
public class GlobalDiscoveryDNS {

    public SocketAddress[] addressArray = null;
    
    public GlobalDiscoveryDNS() {
        //TODO: read from config and get the global ServiceDiscovery SocketAddress
    }
    
    public void setGlobalDiscoveryAddress(SocketAddress[] addresses) {
        this.addressArray = addresses;
    }
    
    public SocketAddress[] getGlobalDiscoveryAddress() {
        return this.addressArray;
    }
    
}
