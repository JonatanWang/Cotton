
package cotton.network;

import java.io.Serializable;
import cotton.network.ServiceRequest;
import java.io.IOException;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork{
    public ServiceRequest sendToService(byte[] data, ServiceChain to) throws IOException;
    //public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder);
}
