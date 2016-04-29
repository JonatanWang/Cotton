package cotton.internalRouting;

import cotton.network.ServiceChain;
import cotton.internalRouting.ServiceRequest;

/**
 *
 * @author Tony
 */
public interface InternalRoutingClient {
    public boolean sendToService(byte[] data,ServiceChain serviceChain);
    public ServiceRequest sendKeepAlive(byte[] data,ServiceChain serviceChain);
    public ServiceRequest sendWithResponse(byte[] data,ServiceChain serviceChain);
    
}
