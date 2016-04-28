
package cotton.network;

import java.io.Serializable;
import java.io.IOException;
import cotton.network.DeprecatedServiceRequest;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork{
    public DeprecatedServiceRequest sendToService(byte[] data, ServiceChain to) throws IOException;
    //public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder);
}
