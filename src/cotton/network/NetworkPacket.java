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

    public NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        if(this.path == null) {
            this.path = new DummyServiceChain();
        }
        this.pt = pt;
        this.keepAlive = false;
    }

    public NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt, boolean keepAlive) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        this.pt = pt;
        this.keepAlive = keepAlive;
    }

    public void setData(byte[] data){
        this.data = data;
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

    public Serializable getData() {
        return data;
    }

    public byte[] getDataBytes() throws IOException{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream writer = new ObjectOutputStream(outputStream);
        writer.writeObject(data);
        return outputStream.toByteArray();
    }

    public boolean keepAlive(){
        return keepAlive;
    }
}
