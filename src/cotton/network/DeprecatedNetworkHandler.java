package cotton.network;

import cotton.services.ServicePacket;
import java.io.Serializable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public interface DeprecatedNetworkHandler extends Runnable {
    public ServicePacket nextPacket();
    public void sendToService(Serializable data, ServiceChain path, ServiceConnection from);
    public DeprecatedServiceRequest sendToService(Serializable data, ServiceChain path);
    public boolean send(Serializable data, ServiceConnection destination);
    public DeprecatedServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException;
    public boolean sendEnd(Serializable data, ServiceConnection destination);
    public void stop();
}
