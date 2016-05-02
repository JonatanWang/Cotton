package cotton.services;

import cotton.network.Origin;
import cotton.network.ServiceChain;


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
     * @param data the data to be used by the service.
     * @param to the <code>ServiceChain</code> of the connection.
     * @return the result of the task.
     */
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to);

}
