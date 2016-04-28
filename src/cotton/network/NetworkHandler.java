package network;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    public boolean send(cotton.network.NetworkPacket netPacket, SocketAddress addr);
    public CountDownLatch sendKeepAlive(cotton.network.NetworkPacket netPacket,SocketAddress addr);
    public void stop();
}
