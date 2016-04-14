package cotton.network;

import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface NetworkHandler {
    
    public ServicePacket nextPacket();
    
    public void sendServiceResult(ServiceConnection from,Serializable result,ServiceChain to);

}
