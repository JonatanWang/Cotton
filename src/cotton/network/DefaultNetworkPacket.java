package cotton.network;

import java.io.Serializable;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 * @author Tony Tran
 */
public class DefaultNetworkPacket implements NetworkPacket{
    private static final long serialVersionUID = 1L;
    private Serializable data;
    private ServiceConnection from;
    private ServiceChain path;
    private PathType pt;

    public DefaultNetworkPacket(Serializable data, ServiceChain path, ServiceConnection from, PathType pt) {
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

    public PathType getType(){
        return pt;
    }

    public Serializable getData() {
        return data;
    }

}
