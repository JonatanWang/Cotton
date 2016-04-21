
package cotton.servicediscovery;

import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ActiveServiceLookup;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Magnus
 */
public class DefaultGlobalServiceDiscovery implements ServiceDiscovery {
    private ActiveServiceLookup internalLookup;
    private NetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool globalDiscovery;
    
    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        this.globalDiscovery = new AddressPool();
        SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
        if(addrArr == null) return;
        for (int i = 0; i < addrArr.length; i++) {
            globalDiscovery.addAddress(addrArr[i]);
        }
    }
    public DefaultGlobalServiceDiscovery(ActiveServiceLookup internalLookup) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.globalDiscovery = new AddressPool();
    }
    
    public DefaultGlobalServiceDiscovery(ActiveServiceLookup internalLookup,GlobalDiscoveryDNS globalDNS) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        initGlobalDiscoveryPool(globalDNS);
    }
    
    
    @Override
    public void setNetwork(NetworkHandler network, SocketAddress localAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void discoveryUpdate(ServiceConnection from, InputStream data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean announce() {
        return false;
    }
    
}
