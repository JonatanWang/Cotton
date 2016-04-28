
package cotton.network;

import java.io.Serializable;
import cotton.network.DeprecatedServiceRequest;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork{
    public DeprecatedServiceRequest sendToService(Serializable data, ServiceChain to);
    //public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder);
}
