package InternalRouting;
import cotton.network.NetworkPacket;
import cotton.network.SocketLatch;

public interface InternalRoutingNetwork {
    public void pushNetworkPacket(NetworkPacket networkPacket);
    public void pushKeepAlivePacket(NetworkPacket networkPacket,SocketLatch latch);
}
