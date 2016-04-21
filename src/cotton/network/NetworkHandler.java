package cotton.network;

import cotton.services.ServicePacket;
import java.io.Serializable;
import java.io.IOException;

/**
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public interface NetworkHandler extends Runnable {
    public ServicePacket nextPacket();
    public void sendToService(Serializable data, ServiceChain path, ServiceConnection from);
    public ServiceRequest sendToService(Serializable data, ServiceChain path);
    public boolean send(Serializable data, ServiceConnection destination);
    public ServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException;
    public void stop();
}
