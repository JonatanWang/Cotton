package cotton.network;

import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface NetworkHandler extends Runnable {
    public ServicePacket nextPacket();
    public ServiceConnection sendToService(Serializable result,ServiceChain to,ServiceConnection from);
    public ServiceConnection sendToService(Serializable result, ServiceChain to);
    public void send(Serializable result, ServiceConnection destination);
}
