
package cotton.internalRouting;

import cotton.network.DestinationMetaData;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.ServiceRequest;
import cotton.network.SocketLatch;

/**
 *
 * @author Magnus
 */
public class DefaultInternalRouting implements InternalRoutingNetwork, InternalRoutingClient,InternalRoutingServiceDiscovery {

    /**
     * The InternalRoutingNetwork implementation
     */
    
    /**
     * 
     * @param networkPacket 
     */
    @Override
    public void pushNetworkPacket(NetworkPacket networkPacket) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param networkPacket
     * @param latch 
     */
    @Override
    public void pushKeepAlivePacket(NetworkPacket networkPacket, SocketLatch latch) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    @Override
    public ServiceRequest sendKeepAlive(byte[] data, ServiceChain serviceChain) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
     * The InternalRoutingNetwork implementation
     */
    
}
