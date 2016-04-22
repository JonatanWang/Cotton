package cotton.servicediscovery;

import cotton.network.NetworkHandler;
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

/**
 *
 * @author Magnus
 */
public class DefaultGlobalServiceDiscovery implements ServiceDiscovery {

    private ActiveServiceLookup internalLookup;
    private NetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool discoveryCache;
    private NetworkHandler networkHandler;
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

    private void startLocalDiscovery(NetworkHandler network, SocketAddress localAddress) {
        SocketAddress[] tmp = new SocketAddress[1];
        tmp[0] = localAddress;
        GlobalDiscoveryDNS discoveryDNSPool = new GlobalDiscoveryDNS();
        discoveryDNSPool.setGlobalDiscoveryAddress(tmp);

        this.localDiscovery = new DefaultLocalServiceDiscovery(internalLookup, discoveryDNSPool);
        this.localDiscovery.setNetwork(network, localAddress);
        this.localDiscovery.announce();     // sends to global discovery (this)
    }

    @Override
    public void setNetwork(NetworkHandler network, SocketAddress localAddress) {
        this.network = network;
        this.localAddress = localAddress;
        startLocalDiscovery(network, localAddress);
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        return this.localDiscovery.getDestination(destination, to);
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        return this.localDiscovery.getDestination(destination, from, to);
    }

    @Override
    public RouteSignal getLocalInterface(ServiceConnection from, ServiceChain to) {
        return this.localDiscovery.getLocalInterface(from, to);
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
        
        ServiceRequest req = null;
        try {
            //from.setPathType(PathType.SERVICE); // bug
            network.send(packet, from);
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
                    localDiscovery.updateHandling(from, packet);
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
