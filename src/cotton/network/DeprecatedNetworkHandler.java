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

    public void sendToService(byte[] data, ServiceChain path, ServiceConnection from) throws IOException;

    public DeprecatedServiceRequest sendToService(byte[] data, ServiceChain path) throws IOException;

    public boolean send(byte[] data, ServiceConnection destination) throws IOException;

    public boolean send(Serializable data, ServiceConnection destination) throws IOException;

    public DeprecatedServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException;

    public boolean sendEnd(byte[] data, ServiceConnection destination);

    public void stop();

}
