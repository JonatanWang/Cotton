package cotton.network;
import cotton.network.NetworkPacket;
import java.util.concurrent.CountDownLatch;
public interface InternalRoutingNetwork {
    public void pushNetworkPacket(NetworkPacket networkPacket);
    public void pushKeepAlivePacket(NetworkPacket networkPacket,CountDownLatch latch);
}
