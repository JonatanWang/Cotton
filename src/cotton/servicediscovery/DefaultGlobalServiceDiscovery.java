
package cotton.servicediscovery;

import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import java.io.InputStream;
import java.net.SocketAddress;

/**
 *
 * @author Magnus
 */
public class DefaultGlobalServiceDiscovery implements ServiceDiscovery {

    @Override
    public void setNetwork(NetworkHandler network, SocketAddress localAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RouteSignal getLocalInterface(ServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void discoveryUpdate(ServiceConnection from, InputStream data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean announce() {
        return false;
    }
    
}
