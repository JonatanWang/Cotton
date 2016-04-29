package cotton.internalRouting;

import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.SocketLatch;
import java.net.SocketAddress;
import cotton.servicediscovery.RouteSignal;
import cotton.servicediscovery.ServiceDiscovery;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkPacket;
import cotton.internalRouting.InternalRoutingServiceHandler;
import cotton.internalRouting.ServiceRequest;
import cotton.network.NetworkHandler;

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
        Origin origin = new Origin();
        return resolveDestination(origin,serviceChain,data,false);
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
        if(resolveDestination(origin,serviceChain,data,true)){
            return request;
        }
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
     private ServiceRequest newServiceRequest(Origin origin) {
         UUID requestID = UUID.randomUUID();
         origin.setServiceRequestID(requestID);
         ServiceRequest requestLatch = new DefaultServiceRequest();
         if (connectionTable.putIfAbsent(requestID, requestLatch) != null) {
             return null;
         }
         return requestLatch;
     }

     /**
     * @param origin The source who inititated the service request
     * @param serviceChain The serviceChain
     * @param data A byte array of data
     * @param keepAlive A boolean whether the service request needs a keep alive socket or not.
     * @param pathType The destination type of the sub system.
     * @return A filled in networkpacket ready for transmission.
     **/

     private NetworkPacket prepareForTransmission(Origin origin, ServiceChain path, byte[] data, PathType pathType) {
         return new NetworkPacket(data, path, origin, pathType);
     }
     /**
     * Forwads the data to correct destination.
     *
     * @param origin The source who inititated the service request
     * @param serviceChain The serviceChain
     * @param data A byte array of data
     * @param keepAlive A boolean whether the service request needs a keep alive socket or not.
     * @return boolean if it is succesfully routed or not.
     **/
     private boolean resolveDestination(Origin origin, ServiceChain serviceChain, byte[] data,boolean keepAlive) {
         boolean success = false;
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
                 if(keepAlive){
                     success = this.networkHandler.sendKeepAlive(packet,dest.getSocketAddress());
                 }else{
                     success = this.networkHandler.send(packet,dest.getSocketAddress());
                 }
                 break;
             case RETURNTOORIGIN:
                 packet = prepareForTransmission(origin,serviceChain,data,dest.getPathType());
                 if(dest.getSocketAddress()==null){
                     SocketLatch socketLatch = keepAliveTable.get(origin.getSocketLatchID());
                     if(socketLatch == null){
                         System.out.println("SocketLatch not found");
                         //TODO: log error
                     }else{
                         socketLatch.setData(packet);
                     }
                 }
                 success = this.networkHandler.send(packet,origin.getAddress());
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
         return success;
     }

}
