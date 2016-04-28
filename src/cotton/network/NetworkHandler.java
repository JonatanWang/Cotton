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

    public void sendToService(byte[] data, ServiceChain path, ServiceConnection from) throws IOException;

    public ServiceRequest sendToService(byte[] data, ServiceChain path) throws IOException;

    public boolean send(byte[] data, ServiceConnection destination) throws IOException;

    public boolean send(Serializable data, ServiceConnection destination) throws IOException;

    public ServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException;

    public boolean sendEnd(byte[] data, ServiceConnection destination);

    public void stop();
}
