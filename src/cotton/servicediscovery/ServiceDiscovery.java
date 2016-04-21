package cotton.servicediscovery;

import cotton.network.NetworkHandler;
import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.net.SocketAddress;

/**
 *@author Mats, Magnus
 */

public interface ServiceDiscovery{
    public void setNetwork(NetworkHandler network, SocketAddress localAddress);
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to); // outgoinging package
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(ServiceConnection from,ServiceChain to); // incoming packaged 
    public void sendUpdate(ServiceConnection from, InputStream data);
}
