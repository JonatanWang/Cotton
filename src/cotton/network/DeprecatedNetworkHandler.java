package cotton.network;

import cotton.services.DeprecatedServicePacket;
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
@Deprecated
public interface DeprecatedNetworkHandler extends Runnable {

    public DeprecatedServicePacket nextPacket();

    public void sendToService(byte[] data, ServiceChain path, DeprecatedServiceConnection from) throws IOException;

    public DeprecatedServiceRequest sendToService(byte[] data, ServiceChain path) throws IOException;

    public boolean send(byte[] data, DeprecatedServiceConnection destination) throws IOException;

    public boolean send(Serializable data, DeprecatedServiceConnection destination) throws IOException;

    public DeprecatedServiceRequest sendWithResponse(Serializable data, DeprecatedServiceConnection destination) throws IOException;

    public boolean sendEnd(byte[] data, DeprecatedServiceConnection destination);

    public void stop();

}
