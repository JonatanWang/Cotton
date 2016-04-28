package network;

import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import cotton.network.SocketLatch;

/**
 *
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    public boolean send(NetworkPacket netPacket, SocketAddress addr);
    public SocketLatch sendKeepAlive(NetworkPacket netPacket,SocketAddress addr);
    public SocketAddress getLocalAddress();
    public void stop();
}
