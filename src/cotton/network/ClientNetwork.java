
package cotton.network;

import java.io.Serializable;
import cotton.network.ServiceRequest;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork{
    public ServiceRequest sendToService(Serializable data, ServiceChain to);
    //public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder);
}
