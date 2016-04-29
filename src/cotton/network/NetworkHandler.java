package network;

import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import cotton.network.SocketLatch;

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
    public boolean send(NetworkPacket netPacket, SocketAddress addr);
    public boolean sendKeepAlive(NetworkPacket netPacket,SocketAddress addr);
    public SocketAddress getLocalAddress();
    public void stop();
}
