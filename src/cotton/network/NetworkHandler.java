package network;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    /*<<<<<<< HEAD

    public ServicePacket nextPacket();

    public void sendToService(byte[] data, ServiceChain path, ServiceConnection from) throws IOException;

    public ServiceRequest sendToService(byte[] data, ServiceChain path) throws IOException;

    public boolean send(byte[] data, ServiceConnection destination) throws IOException;

    public boolean send(Serializable data, ServiceConnection destination) throws IOException;

    public ServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException;

    public boolean sendEnd(byte[] data, ServiceConnection destination);

    =======*/
    public boolean send(cotton.network.NetworkPacket netPacket, SocketAddress addr);

    public CountDownLatch sendKeepAlive(cotton.network.NetworkPacket netPacket,SocketAddress addr);
    //>>>>>>> 3f959a990344bc0537bb67d5c3694bbd7ff02f02
    public void stop();
}
