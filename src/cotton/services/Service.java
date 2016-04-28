package cotton.services;

import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.io.Serializable;

/**
 *
 *
 * @author Tony
 * @author Magnus
 */
public interface Service {

    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to);

}
