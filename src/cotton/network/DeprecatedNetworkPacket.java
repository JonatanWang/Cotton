package cotton.network;

import java.io.Serializable;


/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 * @author Tony Tran
 */
public interface DeprecatedNetworkPacket extends Serializable{
    public ServiceChain getPath();

    public ServiceConnection getOrigin();

    public PathType getType();

    public Serializable getData();

    public boolean keepAlive();
}
