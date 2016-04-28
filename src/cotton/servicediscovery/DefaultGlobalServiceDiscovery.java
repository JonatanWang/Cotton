package cotton.servicediscovery;

import cotton.network.DefaultServiceConnection;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.network.ServiceRequest;
import cotton.services.ActiveServiceLookup;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.network.DeprecatedNetworkHandler;

/**
 *
 * @author Magnus
 */
public class DefaultGlobalServiceDiscovery implements DeprecatedServiceDiscovery {

    private ActiveServiceLookup internalLookup;
    private DeprecatedNetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool discoveryCache;
    private DeprecatedNetworkHandler networkHandler;
    private ExecutorService threadPool;
    private volatile boolean active = true;

    private DefaultLocalServiceDiscovery localDiscovery = null;

    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        this.discoveryCache = new AddressPool();
        SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
        if (addrArr == null) {
            return;
        }
        for (int i = 0; i < addrArr.length; i++) {
            discoveryCache.addAddress(addrArr[i]);
        }
    }

    public DefaultGlobalServiceDiscovery(ActiveServiceLookup internalLookup, GlobalDiscoveryDNS globalDNS) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        initGlobalDiscoveryPool(globalDNS);
        threadPool = Executors.newCachedThreadPool();
    }

    public void stop() {
        threadPool.shutdown();
    }

    private void startLocalDiscovery(DeprecatedNetworkHandler network, SocketAddress localAddress) {
        SocketAddress[] tmp = new SocketAddress[1];
        tmp[0] = localAddress;
        GlobalDiscoveryDNS discoveryDNSPool = new GlobalDiscoveryDNS();
        discoveryDNSPool.setGlobalDiscoveryAddress(tmp);

        this.localDiscovery = new DefaultLocalServiceDiscovery(internalLookup, discoveryDNSPool);
        this.localDiscovery.setNetwork(network, localAddress);
        this.localDiscovery.announce();     // sends to global discovery (this)
    }

    @Override
    public void setNetwork(DeprecatedNetworkHandler network, SocketAddress localAddress) {
        this.network = network;
        this.localAddress = localAddress;
        //startLocalDiscovery(network, localAddress);
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        String key = to.peekNextServiceName();
        if(key == null){return signal;}
        AddressPool pool = serviceCache.get(key);
        
        if(pool == null) {return signal;}
        
        InetSocketAddress addr = null;
        try {
            addr = (InetSocketAddress) pool.getAddress();
            if(addr != null) {
                destination.setAddress(addr);
                destination.setPathType(PathType.SERVICE);
                signal = RouteSignal.NETWORKDESTINATION;
            }
        }catch(NullPointerException ex) {
            signal = RouteSignal.NOTFOUND;
        }
        return signal;
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        String key = to.peekNextServiceName();
        if(key == null){ //service chain is empty, send back to origin 
            if(from == null) {return signal;}
            destination.setAddress(from.getAddress());
            destination.setPathType(from.getPathType());
            ((DefaultServiceConnection)destination).setUserConnectionId(from.getUserConnectionId());
            return RouteSignal.RETURNTOORIGIN;
        }
        AddressPool pool = serviceCache.get(key);
        
        if(pool == null) {return signal;}
        
        InetSocketAddress addr = null;
        try {
            addr = (InetSocketAddress) pool.getAddress();
            if(addr != null) {
                destination.setAddress(addr);
                destination.setPathType(PathType.SERVICE);
                signal = RouteSignal.NETWORKDESTINATION;
            }
        }catch(NullPointerException ex) {
            signal = RouteSignal.NOTFOUND;
        }
        return signal;
    }

    @Override
    public RouteSignal getLocalInterface(ServiceConnection from, ServiceChain to) {
        return RouteSignal.NOTFOUND;
    }

    private DiscoveryPacket packetUnpack(InputStream data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(data);
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }

    private void addService(SocketAddress addr, String name) {
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.serviceCache.putIfAbsent(name, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
    }

    private void processAnnouncePacket(ServiceConnection from, AnnoncePacket packet) {
        String[] serviceList = packet.getServiceList();
        if(from == null) {
            System.out.println("Ip:" + ((InetSocketAddress)from.getAddress()).toString());
            
        }
        for (int i = 0; i < serviceList.length; i++) {
            System.out.println("\tService: " + serviceList[i]);
            addService(from.getAddress(), serviceList[i]);
        }
    }

    private void processProbeRequest(ServiceConnection from, DiscoveryProbe probe) {
        AddressPool pool = this.serviceCache.get(probe.getName());
        if (pool == null) {
            probe.setAddress(null);
        }else {
            probe.setAddress(pool.getAddress());
            
        }
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacket.DiscoveryPacketType.DISCOVERYRESPONSE);
        packet.setProbe(probe);
        from.setPathType(PathType.DISCOVERY);
        ServiceConnection dest = new DefaultServiceConnection(from.getUserConnectionId());
        dest.setAddress(from.getAddress());
        dest.setPathType(PathType.DISCOVERY);
        ServiceRequest req = null;
        try {
            //network.send(packet, from);
            network.send(packet, dest);
        } catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class DiscoveryLookup implements Runnable {

        private ServiceConnection from;
        private InputStream data;

        public DiscoveryLookup(ServiceConnection from, InputStream data) {
            this.from = from;
            this.data = data;
        }

        @Override
        public void run() {
            DiscoveryPacket packet = packetUnpack(data);
            DiscoveryPacket.DiscoveryPacketType type = packet.getPacketType();
            //to do: switch not functioning properly with enums
            System.out.println("DefaultGlobalServiceDiscovery: " + type 
                            + " from: " + ((InetSocketAddress)from.getAddress()).toString());
            switch (type) {
                case DISCOVERYREQUEST:
                    processProbeRequest(from, packet.getProbe());
                    break;
                case DISCOVERYRESPONSE:
                    //localDiscovery.updateHandling(from, packet);
                    break;
                case ANNOUNCE:
                    processAnnouncePacket(from, packet.getAnnonce());
                    //intern handeling method
                    break;
                default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                    System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                    break;
            }
        }

    }

    @Override
    public void discoveryUpdate(ServiceConnection from, InputStream data) {
        DiscoveryLookup th = new DiscoveryLookup(from, data);
        threadPool.execute(th);

    }

    @Override
    public boolean announce() {
        return false;
    }

}
