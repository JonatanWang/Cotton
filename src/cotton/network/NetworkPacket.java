package cotton.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author tony
 * @author Magnus
 */
public class NetworkPacket implements Serializable{
    private static final long serialVersionUID = 1L;
    private byte[] data;
    private Origin origin;
    private ServiceChain path;
    private PathType pt;
    private boolean keepAlive;

    private NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        if(this.path == null) {
            this.path = new DummyServiceChain();
        }
        this.pt = pt;
        this.keepAlive = false;
    }

    private NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt, boolean keepAlive) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        this.pt = pt;
        this.keepAlive = keepAlive;
    }

    private NetworkPacket(NetworkPacketBuilder builder){
        this.data = builder.data;
        this.origin = builder.origin;
        this.path = builder.path;
        this.pt = builder.pt;
        this.keepAlive = builder.keepAlive;
    }

    public ServiceChain getPath(){
        return path;
    }

    public Origin getOrigin(){
        return origin;
    }

    public PathType getType(){
        return pt;
    }

    public byte[] getData() {
        return data;
    }

    public boolean keepAlive(){
        return keepAlive;
    }

    public void removeData(){
        data = null;
    }

    public static NetworkPacketBuilder newBuilder(){
        return new NetworkPacketBuilder();
    }

    public static class NetworkPacketBuilder{
        byte[] data;
        Origin origin;
        ServiceChain path;
        PathType pt;
        boolean keepAlive;

        public NetworkPacketBuilder(){
            data = null;
            origin = null;
            path = null;
            pt = null;
            keepAlive = false;
        }

        public NetworkPacketBuilder setData(byte[] data){
            this.data = data;
            return this;
        }

        public NetworkPacketBuilder setOrigin(Origin origin){
            this.origin = origin;
            return this;
        }

        public NetworkPacketBuilder setPath(ServiceChain path){
            this.path = path;
            return this;
        }

        public NetworkPacketBuilder setPathType(PathType pt){
            this.pt = pt;
            return this;
        }

        public NetworkPacketBuilder setKeepAlive(boolean keepAlive){
            this.keepAlive = keepAlive;
            return this;
        }

        public NetworkPacket build(){
            return new NetworkPacket(this);
        }

    }
}
