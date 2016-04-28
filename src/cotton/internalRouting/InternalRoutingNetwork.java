package cotton.internalRouting;
import cotton.network.SocketLatch;
import cotton.network.NetworkPacket;

public interface InternalRoutingNetwork {
    public void pushNetworkPacket(NetworkPacket networkPacket);
    public void pushKeepAlivePacket(NetworkPacket networkPacket,SocketLatch latch);
}
