package cotton.services;

import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.io.Serializable;

/**
 * The <code>ServiceInstance</code> acts as the structure for services that are
 * to be implemented by the <code>ServiceFactory</code>.
 *
 * @author Tony
 * @author Magnus
 * @see ServiceFactory
 */
public interface Service {

    /**
     * Runs the service task and returns the result as a <code>Serializable</code>.
     *
     * @param ctx contains the cloud context.
     * @param from the information about the connection.
     * @param data the data to be used by the service.
     * @param to the <code>ServiceChain</code> of the connection.
     * @return the result of the task.
     */
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to);

}
