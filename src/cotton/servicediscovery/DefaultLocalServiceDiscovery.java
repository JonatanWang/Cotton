
package cotton.servicediscovery;

import cotton.network.DefaultServiceConnection;
import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceConnection;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import cotton.services.ServiceMetaData;

/**
 *
 * @author Magnus
 */
public class DefaultLocalServiceDiscovery implements LocalServiceDiscovery {
    private ActiveServiceLookup internalLockup;
    private NetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool globalDiscovery ;

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        destination.setAddress(localAddress);
        return getDestination(destination,destination,to);
    }
    
    private class AddressPool{

        private int pos = 0;
        private ArrayList<SocketAddress> pool= new ArrayList<SocketAddress>();

        public boolean addAddress(SocketAddress address){

            synchronized(this){
                pool.add(address);
            }
            return true;
        }

        public SocketAddress getAddress(){
            SocketAddress addr = null;
            synchronized(this){
                pos = pos % pool.size();

                if(pool.isEmpty() == false){

                    addr = pool.get(pos);
                    pos++;
                }
            }

            return addr;
        }

    }

    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLockup) {
        this.internalLockup = internalLockup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.globalDiscovery = new AddressPool();
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
    
    
    private RouteSignal getGlobalAddress(ServiceConnection destination, String serviceName) {
        return RouteSignal.NOTFOUND;
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        String serviceName;

        serviceName = to.getCurrentServiceName();

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
