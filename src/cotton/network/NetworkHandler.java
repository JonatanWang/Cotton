package cotton.network;

import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface NetworkHandler {
    public ServicePacket nextPacket();
    public void sendToService(ServiceConnection from,Serializable result,ServiceChain to);
    public void sendToTarget(Serializable result, ServiceChain to);
}
