package cotton.servicediscovery;

import cotton.services.ServiceConnection;
import cotton.services.ServiceChain;
import java.io.InputStream;

/**
 *@author Mats, Magnus
 */

public interface LocalServiceDiscovery{

    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to);

    public void sendUpdate(ServiceConnection from, InputStream data, ServiceChain to);
}
