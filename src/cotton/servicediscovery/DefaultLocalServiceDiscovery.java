
package cotton.servicediscovery;

import cotton.network.DefaultServiceConnection;
import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.network.ServiceRequest;
import cotton.services.ActiveServiceLookup;
import cotton.network.ServiceConnection;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import cotton.services.ServiceMetaData;
import java.util.UUID;
import cotton.network.PathType;

/**
 *
 * @author Magnus, Mats
 */
public class DefaultLocalServiceDiscovery implements ServiceDiscovery {
    private ActiveServiceLookup internalLockup;
    private NetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool globalDiscovery ;

    

    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        this.globalDiscovery = new AddressPool();
        SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
        if(addrArr == null) return;
        for (int i = 0; i < addrArr.length; i++) {
            globalDiscovery.addAddress(addrArr[i]);
        }
    }
    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLockup) {
        this.internalLockup = internalLockup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.globalDiscovery = new AddressPool();
    }
    
    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLockup,GlobalDiscoveryDNS globalDNS) {
        this.internalLockup = internalLockup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        initGlobalDiscoveryPool(globalDNS);
    }

    public void setNetwork(NetworkHandler network, SocketAddress localAddress) {
        this.network = network;
        this.localAddress = localAddress;
    }

    private RouteSignal getReturnAddress(ServiceConnection destination, ServiceConnection from) {
        if(from == null){
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(from.getAddress());
        return (from.getAddress().equals(localAddress)) ? RouteSignal.LOCALDESTINATION : RouteSignal.NETWORKDESTINATION;
        
    }
    
    private void cacheAddress(String serviceName,SocketAddress targetAddr) {
        AddressPool poolCheck = serviceCache.get(serviceName);
        if(poolCheck != null) {
            poolCheck.addAddress(targetAddr);
            return;
        }
        AddressPool newPool = new AddressPool();
        newPool.addAddress(targetAddr);
        poolCheck = serviceCache.putIfAbsent(serviceName, newPool);
        if(poolCheck != null) { // the above is "atomic" so if a pool already exist now then use it
            poolCheck.addAddress(targetAddr);
        }
    }
    
    private RouteSignal getGlobalAddress(ServiceConnection destination, String serviceName) {
        SocketAddress addr = this.globalDiscovery.getAddress();
        if(addr == null){
            return RouteSignal.NOTFOUND;
        }
        DefaultServiceConnection globalDest = new DefaultServiceConnection(UUID.randomUUID());
        globalDest.setPathType(PathType.DISCOVERY);
        DiscoveryProbe discoveryProbe = new DiscoveryProbe(serviceName,null);

        globalDest.setAddress(addr);
        ServiceRequest req = network.send(discoveryProbe, globalDest);

        DiscoveryProbe answers = (DiscoveryProbe)req.getData(); //TODO: io checks
        SocketAddress targetAddr = answers.getAddress();
        if(targetAddr == null) {
            return RouteSignal.NOTFOUND;
        }
        
        destination.setAddress(targetAddr);
        cacheAddress(serviceName,targetAddr);
        return RouteSignal.NETWORKDESTINATION;
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        if(destination == null) {
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(localAddress);
        return getDestination(destination,destination,to);
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        if(destination == null) {
            return RouteSignal.NOTFOUND;
        }
        String serviceName;

        serviceName = to.peekNextServiceName();

        if(serviceName == null){
            return getReturnAddress(destination, from);
        }

        ServiceMetaData serviceInfo = internalLockup.getService(serviceName);
        if(serviceInfo != null){
            return RouteSignal.LOCALDESTINATION;
        }
        
        AddressPool pool  = serviceCache.get(serviceName);
        if(pool == null) {
            // get global sd
            return getGlobalAddress(destination,serviceName);
        }
        
        destination.setAddress(pool.getAddress());
        return RouteSignal.NETWORKDESTINATION;

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
