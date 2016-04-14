
package cotton.network;

import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface ClientNetwork <T extends Serializable> {
    public ServiceConnection sendServiceRequest(T data, ServiceChain to);
    public T getResults(ServiceConnection requestId, StreamDecoder<T> decoder);
}
