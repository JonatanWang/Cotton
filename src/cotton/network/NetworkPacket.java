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
    private ServiceChain path;
    private PacketType pt;

    public NetworkPacket(Serializable data, ServiceChain path, ServiceConnection from, PacketType pt) {
        this.data = data;
        this.from = from;
        this.path = path;
        this.pt = pt;
    }

    public ServiceChain getPath(){
        return path;
    }

    public ServiceConnection getOrigin(){
        return from;
    }

    public PacketType getType(){
        return pt;
    }

    public Serializable getData() {
        return data;
    }

    public enum PacketType {
        SERVICE, SERVICE_UPDATE, UNKNOWN
    }
}
