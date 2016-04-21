
package cotton.network;

import java.io.Serializable;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 * @author Tony Tran
 */
public class NetworkPacket implements Serializable{
    private Serializable data;
    private ServiceConnection from;
    private ServiceChain to;
    private PacketType pt;

    public NetworkPacket(Serializable data, ServiceChain to, ServiceConnection from, PacketType pt) {
        this.data = data;
        this.from = from;
        this.to = to;
        this.pt = pt;
    }

    public Serializable getData() {
        return data;
    }

    public enum PacketType {
        SERVICE, SERVICE_UPDATE, UNKNOWN
    }
}
