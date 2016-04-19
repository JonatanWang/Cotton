package cotton.network;

import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.io.Serializable;

/**
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public interface NetworkHandler extends Runnable {
    public ServicePacket nextPacket();
    public void sendToService(Serializable result,ServiceChain to,ServiceConnection from);
    public ServiceRequest sendToService(Serializable result, ServiceChain to);
    public ServiceRequest send(Serializable result, ServiceConnection destination);
}
