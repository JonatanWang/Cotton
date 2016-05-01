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

/**
 *
 * @author magnus
 */
public class GlobalServiceDiscovery implements ServiceDiscovery {

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;
    private AddressPool discoveryCache;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private ActiveServiceLookup localServiceTable = null;

    /**
     * Fills in a list of all pre set globalServiceDiscovery addresses
     * @param globalDNS information from the config file
     */
    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        if (globalDNS != null) {
            SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
            for (int i = 0; i < addrArr.length; i++) {
                discoveryCache.addAddress(addrArr[i]);
            }
        }
    }

    public GlobalServiceDiscovery(GlobalDiscoveryDNS dnsConfig) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(dnsConfig);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
    }

    /**
     * So we can get out to other machines
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
     * @param serviceTable a table with all the local services that can be run on this machine
     */
    @Override
    public void setLocalServiceTable(ActiveServiceLookup serviceTable) {
        this.localServiceTable = serviceTable;
    }

    /**
     * Search globaly for a destination with a service named serviceName
     * @param destination in/out gets filled in with the address and pathtype
     * @param serviceName name of the service to search for.
     * @return
     */
    private RouteSignal searchForService(DestinationMetaData destination, String serviceName) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        // TODO: ask the other GD
        return signal;
    }

    /**
     * Finds the next hop destination, fills in destination, and return a RouteSignal
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
            destination.setSocketAddress(origin.getAddress());
            destination.setPathType(PathType.RELAY);// latch is a relay??
            return signal;
        }

        // TODO: check load factors and so on..
        if(this.localServiceTable != null && localServiceTable.getService(serviceName) != null) {
            signal = RouteSignal.LOCALDESTINATION;
            destination.setPathType(PathType.SERVICE);
            return signal;
        }
        // this is a GlobalServiceDiscovery so check cache first
        AddressPool pool = this.serviceCache.get(serviceName);
        if (pool == null) {
            return searchForService(destination, serviceName);
        }

        SocketAddress addr = pool.getAddress();
        if (addr != null) {
            destination.setSocketAddress(addr);
            destination.setPathType(PathType.SERVICE); // default pathType for now
            signal = RouteSignal.NETWORKDESTINATION;
        }

        return signal;
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

    @Override
    public boolean announce() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stop() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void discoveryUpdate(Origin origin, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private DiscoveryPacket packetUnpack(byte[] data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }

    private class DiscoveryLookup implements Runnable {

        private Origin origin;
        private byte[] data;

        public DiscoveryLookup(Origin origin, byte[] data) {
            this.origin = origin;
            this.data = data;
        }

        @Override
        public void run() {
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
                    //processAnnouncePacket(origin, packet.getAnnounce());
                    //intern handeling method
                    break;
                default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                    System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                    break;
            }
        }
    }
}
