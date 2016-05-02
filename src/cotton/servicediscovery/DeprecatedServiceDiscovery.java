package cotton.servicediscovery;

import cotton.network.ServiceChain;
import java.io.InputStream;
import java.net.SocketAddress;
import cotton.network.DeprecatedNetworkHandler;
import cotton.network.DeprecatedServiceConnection;

/**
 *@author Mats, Magnus
 */
@Deprecated
public interface DeprecatedServiceDiscovery{
    public void setNetwork(DeprecatedNetworkHandler network, SocketAddress localAddress);
    public RouteSignal getDestination(DeprecatedServiceConnection destination, ServiceChain to); // outgoinging package
    public RouteSignal getDestination(DeprecatedServiceConnection destination, DeprecatedServiceConnection from, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(DeprecatedServiceConnection from,ServiceChain to); // incoming packaged 
    public boolean announce();
    public void stop();
    public void discoveryUpdate(DeprecatedServiceConnection from, byte[] data);
}
