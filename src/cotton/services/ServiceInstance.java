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
public interface ServiceInstance {

    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to);

}
