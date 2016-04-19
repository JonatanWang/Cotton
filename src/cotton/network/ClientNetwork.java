
package cotton.network;

import cotton.services.ServiceConnection;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork{
    public ServiceRequest sendToService(Serializable data, ServiceChain to);
    //public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder);
}
