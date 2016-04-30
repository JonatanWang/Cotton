package cotton.servicediscovery;

import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class GlobalServiceDiscovery implements ServiceDiscovery {

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;

    @Override
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress) {
        this.internalRouting = network;
        this.localAddress = localAddress;
    }

    @Override
    public RouteSignal getDestination(DestinationMetaData destination, Origin origin, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This is used when a packet have arrived at the origin point,
     * If this is a keepalive bridge it gives back a networkdestination, 
     * else indicate if it should go to a local subsystem or fill a serviceRequest
     * @param origin must be this machine (localAddress) and not null
     * @return ENDPOINT,LOCALDESTINATION,NETWORKDESTINATION
     */
    private RouteSignal resolveLocalEndpoint(Origin origin) {
        if(origin.getSocketLatchID() != null) {
            return RouteSignal.NETWORKDESTINATION; // we are a nat bridge
        }
        if(origin.getServiceRequestID() == null) {
            return RouteSignal.LOCALDESTINATION;
        }
        return RouteSignal.ENDPOINT;
    }
    
    /**
     * This is used when a packet have arrived and its serviceChain is empty
     * It will then determine how to route the incoming data
     * @param origin the origin part of the packet
     * @return RouteSignal
     */
    private RouteSignal resolveOriginRoute(Origin origin) {
        if (origin == null) {
            return RouteSignal.NOTFOUND;
        }

        InetSocketAddress address = (InetSocketAddress) origin.getAddress();
        // when Origin address is null and ServiceRequestID exist we just past
        if (address == null) {
            if (origin.getServiceRequestID() != null) {
                return RouteSignal.ENDPOINT;
            }
            return RouteSignal.LOCALDESTINATION;    // assums that its a oneway msg
        }
        
        if (address.equals((InetSocketAddress) localAddress)) {
            return resolveLocalEndpoint(origin);
        }
        return RouteSignal.RETURNTOORIGIN;
    }

    /**
     * This is used when a packet have arrived and gives back the correct route
     * @param origin where the packet came from
     * @param to where the packet should go
     * @return where to route the data
     */
    @Override
    public RouteSignal getLocalInterface(Origin origin, ServiceChain to) {
        String nextService = to.peekNextServiceName();
        if (nextService == null) {
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
