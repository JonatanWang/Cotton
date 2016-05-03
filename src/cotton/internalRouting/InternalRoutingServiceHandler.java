
package cotton.internalRouting;

import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.ServiceBuffer;

/**
 *
 * @author tony
 */
public interface InternalRoutingServiceHandler {
    public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result);
    public ServiceBuffer getServiceBuffer();
    public boolean notifyRequestQueue(String serviceName);
}
