package cotton.servicediscovery;

import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.net.SocketAddress;
import cotton.network.DeprecatedNetworkHandler;

/**
 *@author Mats, Magnus
 */

public interface DeprecatedServiceDiscovery{
    public void setNetwork(DeprecatedNetworkHandler network, SocketAddress localAddress);
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to); // outgoinging package
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(ServiceConnection from,ServiceChain to); // incoming packaged 
    public boolean announce();
    public void stop();
    public void discoveryUpdate(ServiceConnection from, InputStream data);
}
