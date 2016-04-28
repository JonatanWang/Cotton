package cotton.servicediscovery;

import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import java.net.SocketAddress;

/**
 *
 * @author Magnus
 */
public interface ServiceDiscovery {
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress);
    public RouteSignal getDestination(DestinationMetaData destination, Origin origin, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(Origin origin, ServiceChain to); // incoming packaged
    public boolean announce();
    public void stop();
    public void discoveryUpdate(Origin origin, byte[] data);
}
