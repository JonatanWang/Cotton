
package cotton.servicediscovery;

import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceConnection;
import java.io.InputStream;

/**
 *
 * @author Magnus
 */
public class DefaultLocalServiceDiscovery implements LocalServiceDiscovery {
    private ActiveServiceLookup internalLockup;
    private NetworkHandler network = null;
    
    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLockup) {
        this.internalLockup = internalLockup;
    }

    public void setNetwork(NetworkHandler network) {
        this.network = network;
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RouteSignal getLocalInterface(ServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendUpdate(ServiceConnection from, InputStream data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
