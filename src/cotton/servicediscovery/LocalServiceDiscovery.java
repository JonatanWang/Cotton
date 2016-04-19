package cotton.servicediscovery;

import cotton.services.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;

/**
 *@author Mats, Magnus
 */

public interface LocalServiceDiscovery{

    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(ServiceConnection from,ServiceChain to); // incoming packaged 
    public void sendUpdate(ServiceConnection from, InputStream data);
}
