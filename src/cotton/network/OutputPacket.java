package cotton.network;

import java.net.SocketAddress;

/**
 * Created by jonathan on 18/05/16.
 */
public class OutputPacket {
    private NetworkPacket packet;
    private SocketAddress destination;
    private boolean keepAlive;

    public OutputPacket(NetworkPacket packet, SocketAddress destination, boolean keepAlive) {
        this.packet = packet;
        this.destination = destination;
        this.keepAlive = keepAlive;
    }

    public NetworkPacket getPacket() {
        return packet;
    }

    public SocketAddress getDestination() {
        return destination;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }
}
