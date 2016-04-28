package cotton.network;

import java.io.Serializable;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 * @author Tony Tran
 */
public class DeprecatedDefaultNetworkPacket implements DeprecatedNetworkPacket{
    private static final long serialVersionUID = 1L;
    private Serializable data;
    private ServiceConnection from;
    private ServiceChain path;
    private PathType pt;
    private boolean keepAlive;

    public DeprecatedDefaultNetworkPacket(Serializable data, ServiceChain path, ServiceConnection from, PathType pt) {
        this.data = data;
        this.from = from;
        this.path = path;
        if(this.path == null) {
            this.path = new DummyServiceChain();
        }
        this.pt = pt;
        this.keepAlive = false;
    }

    public DeprecatedDefaultNetworkPacket(Serializable data, ServiceChain path, ServiceConnection from, PathType pt, boolean keepAlive) {
        this.data = data;
        this.from = from;
        this.path = path;
        this.pt = pt;
        this.keepAlive = keepAlive;
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

    public boolean keepAlive(){
        return keepAlive;
    }

}
