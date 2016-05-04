package cotton.servicediscovery;

import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import cotton.services.ActiveServiceLookup;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import cotton.services.ServiceMetaData;
import java.util.ArrayList;
import java.io.Serializable;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DestinationMetaData;

/**
 *
 * @author magnus
 * @author Tony
 */
public class LocalServiceDiscovery implements ServiceDiscovery {

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;
    private AddressPool discoveryCache;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private ActiveServiceLookup localServiceTable = null;
    private ConcurrentHashMap<String, AddressPool> activeQueue;

    /**
     * Fills in a list of all pre set globalServiceDiscovery addresses
     *
     * @param globalDNS information from the config file
     */
    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        if (globalDNS != null) {
            SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
            for (int i = 0; i < addrArr.length; i++) {
                DestinationMetaData gAddr = new DestinationMetaData(addrArr[i],PathType.DISCOVERY); 
                discoveryCache.addAddress(gAddr);
            }
        }
    }

    public LocalServiceDiscovery(GlobalDiscoveryDNS dnsConfig) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(dnsConfig);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.activeQueue = new ConcurrentHashMap<>();
    }

    /**
     * So we can get out to other machines
     *
     * @param network GlobalServiceDiscovery way out to the world
     * @param localAddress what machine its one and its port
     */
    @Override
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress) {
        this.internalRouting = network;
        this.localAddress = localAddress;
    }

    /**
     * If this machine runs local services set the lookup table
     *
     * @param serviceTable a table with all the local services that can be run
     * on this machine
     */
    @Override
    public void setLocalServiceTable(ActiveServiceLookup serviceTable) {
        this.localServiceTable = serviceTable;
    }

    /**
     * Search globaly for a destination with a service named serviceName
     *
     * @param destination in/out gets filled in with the address and pathtype
     * @param serviceName name of the service to search for.
     * @return
     */
    private RouteSignal searchForService(DestinationMetaData destination, String serviceName) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        // TODO: ask the other GD
        DiscoveryProbe probe = new DiscoveryProbe(serviceName, null);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
        packet.setProbe(probe);

        DestinationMetaData dest = discoveryCache.getAddress();
        //DestinationMetaData dest = new DestinationMetaData(addr,PathType.DISCOVERY);
        try {
            byte[] data = serializeToBytes(packet);
            ServiceRequest request = internalRouting.sendWithResponse(dest, data);
            if (request == null) {
                return RouteSignal.NOTFOUND;
            }
            DiscoveryPacket discoveryPacket = packetUnpack(request.getData());
            DiscoveryProbe discoveryProbe = discoveryPacket.getProbe();
            DestinationMetaData destMeta = null;
            if (discoveryProbe != null && (destMeta = discoveryProbe.getAddress()) != null) {
                destination.setSocketAddress(destMeta.getSocketAddress());
                destination.setPathType(destMeta.getPathType());
                return RouteSignal.NETWORKDESTINATION;
            }
        } catch (IOException ex) {
        }

        return signal;
    }

    /**
     * Finds the next hop destination, fills in destination, and return a
     * RouteSignal
     *
     * @param destination in/out gets filled in with the address and pathType
     * @param origin where this msg/packet came from
     * @param to a chain of services that should be visited next
     * @return where to reach the destinatino, local,net,nat,endpoint
     */
    @Override
    public RouteSignal getDestination(DestinationMetaData destination, Origin origin, ServiceChain to) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        String serviceName = null;
        if (to != null) {
            serviceName = to.peekNextServiceName();
        }
        if (serviceName == null) {
            signal = resolveOriginRoute(origin);
            if (signal == RouteSignal.NOTFOUND) {
                return signal;
            }
            destination.setSocketAddress(origin.getAddress());
            destination.setPathType(PathType.RELAY);// latch is a relay??
            return signal;
        }

        // TODO: check load factors and so on..
        if (this.localServiceTable != null && localServiceTable.getService(serviceName) != null) {
            signal = RouteSignal.LOCALDESTINATION;
            destination.setPathType(PathType.SERVICE);
            return signal;
        }
        // this is a GlobalServiceDiscovery so check cache first
        /*        AddressPool pool = this.serviceCache.get(serviceName);
        if (pool == null) {
            return searchForService(destination, serviceName);
            }*/
        return searchForService(destination, serviceName);
        /*   SocketAddress addr = pool.getAddress();
        if (addr != null) {
            destination.setSocketAddress(addr);
            destination.setPathType(PathType.SERVICE); // default pathType for now
            signal = RouteSignal.NETWORKDESTINATION;
        }

        return signal;*/
    }

    /**
     * This is used when a packet have arrived at the origin point, If this is a
     * keepalive bridge it gives back a networkdestination, else indicate if it
     * should go to a local subsystem or fill a serviceRequest
     *
     * @param origin must be this machine (localAddress) and not null
     * @return ENDPOINT,LOCALDESTINATION,NETWORKDESTINATION
     */
    private RouteSignal resolveLocalEndpoint(Origin origin) {
        if (origin.getSocketLatchID() != null) {
            return RouteSignal.BRIDGELATCH; // we are a nat bridge
        }
        if (origin.getServiceRequestID() == null) {
            return RouteSignal.LOCALDESTINATION;
        }
        return RouteSignal.ENDPOINT;
    }

    /**
     * This is used when a packet have arrived and its serviceChain is empty It
     * will then determine how to route the incoming data
     *
     * @param origin the origin part of the packet
     * @return RouteSignal
     */
    private RouteSignal resolveOriginRoute(Origin origin) {
        if (origin == null) {
            return RouteSignal.NOTFOUND;
        }

        InetSocketAddress address = (InetSocketAddress) origin.getAddress();

        if (address == null || address.equals((InetSocketAddress) localAddress)) {
            return resolveLocalEndpoint(origin);
        }
        return RouteSignal.RETURNTOORIGIN;
    }

    /**
     * This is used when a packet have arrived and gives back the correct route
     *
     * @param origin where the packet came from
     * @param to where the packet should go
     * @return where to route the data
     */
    @Override
    public RouteSignal getLocalInterface(Origin origin, ServiceChain to) {
        if (to == null || to.peekNextServiceName() == null) {
            return resolveOriginRoute(origin);
        }
        // TODO: add check for internal active services on this machine
        return RouteSignal.LOCALDESTINATION;
    }

    private void printAnnounceList(String[] nameList) {
        System.out.println("Service list");
        for (String s : nameList) {
            System.out.println("\t" + s);
        }
    }

    @Override
    public boolean announce() {
        if (localServiceTable == null) {
            return false;
        }
//        InetSocketAddress destAddr = (InetSocketAddress) discoveryCache.getAddress();
//        if(destAddr == null)
//            return false;
        DestinationMetaData dest = discoveryCache.getAddress();
        if (dest == null) {
            return false;
        }
        KeySetView<String, ServiceMetaData> keys = localServiceTable.getKeySet();
        ArrayList<String> serviceList = new ArrayList<>();
        for (String key : keys) {
            serviceList.add(key);
        }
        String[] nameList = serviceList.toArray(new String[serviceList.size()]);
        AnnouncePacket announce = new AnnouncePacket(localAddress, nameList);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.ANNOUNCE);
        packet.setAnnonce(announce);
        printAnnounceList(nameList);
        //DestinationMetaData dest = new DestinationMetaData(destAddr, PathType.DISCOVERY);
        try {
            byte[] bytes = serializeToBytes(packet);
            internalRouting.SendToDestination(dest, bytes);
        } catch (IOException ex) {
            return false;
        }
        return true;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    @Override
    public void stop() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Handles updates messages from global discovery.
     *
     */
    @Override
    public void discoveryUpdate(Origin origin, byte[] data) {
        DiscoveryPacket packet = packetUnpack(data);
        DiscoveryPacketType type = packet.getPacketType();
        //to do: switch not functioning properly with enums
        System.out.println("DefaultGlobalServiceDiscovery: " + type
                + " from: " + ((InetSocketAddress) origin.getAddress()).toString());
        switch (type) {
            case DISCOVERYREQUEST:
                //processProbeRequest(origin, packet.getProbe());
                break;
            case DISCOVERYRESPONSE:
                //localDiscovery.updateHandling(from, packet);
                break;
            case ANNOUNCE:
                //processAnnouncePacket(packet.getAnnounce());
                break;
            default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                break;
        }
    }

    private DiscoveryPacket packetUnpack(byte[] data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(LocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }

    private void addService(DestinationMetaData addr, String name) {
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.serviceCache.putIfAbsent(name, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
    }

    // register announced services in global service discovery
    private void processAnnouncePacket(AnnouncePacket packet) {
        String[] serviceList = packet.getServiceList();
        SocketAddress addr = packet.getInstanceAddress();
        if (addr == null) {
            return;
        }
        // TODO: implement logic redirecting services to their request queue
        DestinationMetaData sAddr = new DestinationMetaData(addr,PathType.SERVICE);
        for (String s : serviceList) {
            addService(sAddr, s);
        }
        printAnnounceList(serviceList);
    }

    private void addQueue(DestinationMetaData addr, String queue) {
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.activeQueue.putIfAbsent(queue, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
    }

    private void processQueuePacket(QueuePacket packet) {
        String[] queueList = packet.getRequestQueueList();
        SocketAddress addr = packet.getInstanceAddress();
        DestinationMetaData qAddr = new DestinationMetaData(addr,PathType.REQUESTQUEUE); 
        if (addr == null) {
            return;
        }
        for (String s : queueList) {
            addQueue(qAddr, s);
        }
    }

    /**
     * Finds the destination to the request queue.
     *
     * @param destination destination the destination address for the
     * queueList
     * @param serviceName serviceName the name for the service that needs new work.
     */
    @Override
    public RouteSignal getRequestQueueDestination(DestinationMetaData destination, String serviceName) {
        AddressPool pool = activeQueue.get(serviceName);
        if(pool == null)
            return RouteSignal.NOTFOUND;
        DestinationMetaData addr = pool.getAddress();
        if (addr == null || addr.getSocketAddress() == null){
            return RouteSignal.NOTFOUND;
        }
        InetSocketAddress socketAddress = (InetSocketAddress)addr.getSocketAddress();
        if(!socketAddress.equals((InetSocketAddress) localAddress)) {
            return RouteSignal.LOCALDESTINATION;
        }
    
        destination.setSocketAddress(addr.getSocketAddress());
        destination.setPathType(addr.getPathType());
        return RouteSignal.NETWORKDESTINATION;
    }

}
