package cotton.internalRouting;

import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.SocketLatch;
import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import cotton.servicediscovery.RouteSignal;
import cotton.servicediscovery.ServiceDiscovery;
import internalRouting.InternalRoutingServiceHandler;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkHandler;
import internalRouting.ServiceRequest;
import cotton.network.NetworkPacket;

/**
 *
 * @author Magnus
 */
public class DefaultInternalRouting implements InternalRoutingNetwork, InternalRoutingClient, InternalRoutingServiceDiscovery, InternalRoutingServiceHandler {

    private NetworkHandler networkHandler;
    private SocketAddress localAddress;
    private ServiceDiscovery discovery;
    private ConcurrentHashMap<UUID, SocketLatch> keepAliveTable;
    private ConcurrentHashMap<UUID, ServiceRequest> connectionTable;
    private ConcurrentLinkedQueue<NetworkPacket> routingQueue;

    public DefaultInternalRouting(NetworkHandler networkHandler, ServiceDiscovery discovery) {
        this.networkHandler = networkHandler;
        this.localAddress = networkHandler.getLocalAddress();
        this.discovery = discovery;
        this.keepAliveTable = new ConcurrentHashMap<>();
        this.routingQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * The InternalRoutingNetwork implementation
     */
    /**
     *
     * @param networkPacket
     */
    @Override
    public void pushNetworkPacket(NetworkPacket networkPacket) {
        routingQueue.add(networkPacket);
    }

    /**
     *
     * @param networkPacket
     * @param latch
     */
    @Override
    public void pushKeepAlivePacket(NetworkPacket networkPacket, SocketLatch latch) {
        UUID latchID = UUID.randomUUID();
        if (keepAliveTable.putIfAbsent(latchID, latch) != null) {
            networkPacket.setData(null);
            latch.setFailed(networkPacket);
            return;
        }
        networkPacket.getOrigin().setAddress(localAddress);
        networkPacket.getOrigin().setSocketLatchID(latchID);
        routingQueue.add(networkPacket);

        //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ServiceRequest newServiceRequest(Origin origin) {
        UUID requestID = UUID.randomUUID();
        origin.setServiceRequestID(requestID);
        ServiceRequest requestLatch = new DefaultServiceRequest();
        if (connectionTable.putIfAbsent(requestID, requestLatch) != null) {
            return null;
        }
        return requestLatch;
    }

    private NetworkPacket prepareForTransmission(Origin origin, ServiceChain path, byte[] data, PathType pathType) {
        return new NetworkPacket(data, path, origin, pathType);
    }

    private boolean resolveDestination(Origin origin, ServiceChain serviceChain, byte[] data,boolean keepAlive) {
        DestinationMetaData dest = new DestinationMetaData();
        RouteSignal route = discovery.getDestination(dest, origin, serviceChain);
        NetworkPacket packet = null;
        switch (route) {
            case LOCALDESTINATION:
                packet = prepareForTransmission(origin,serviceChain,data,dest.getPathType());
                routingQueue.add(packet);
                break;
            case NETWORKDESTINATION:
                packet = prepareForTransmission(origin,serviceChain,data,dest.getPathType());
                
                break;
            case RETURNTOORIGIN:
                break;
            case ENDPOINT:
                DefaultServiceRequest request = (DefaultServiceRequest)connectionTable.get(origin.getServiceRequestID());
                if(request != null){
                    request.setData(data);
                 
                }
                break;
            case NOTFOUND:
                break;

            default:
                System.out.println("Stuff is not working");
                break;
            //TODO: implement error/logging
        }
        return false;
    }

    /**
     * The InternalRoutingClient implementation
     */
    /**
     *
     * @param data
     * @param serviceChain
     * @return
     */
    @Override
    public boolean sendToService(byte[] data, ServiceChain serviceChain) {

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param data
     * @param serviceChain
     * @return
     */
    @Override
    public ServiceRequest sendKeepAlive(byte[] data, ServiceChain serviceChain) {
        Origin origin = new Origin();
        ServiceRequest request = newServiceRequest(origin);

        return null;

        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceRequest sendWithResponse(byte[] data, ServiceChain serviceChain) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * The InternalRoutingServiceDiscovery implementation
     */
    @Override
    public boolean SendBackToOrigin(Origin origin, PathType pathType, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean SendToDestination(DestinationMetaData dest, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceRequest sendWithResponse(DestinationMetaData dest, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * The InternalRoutingServiceHandler implementation
     */
    @Override
    public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * The InternalRoutingNetwork implementation
     */
}
