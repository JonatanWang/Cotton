package cotton.services;

/**
 * Implementing this interface allows a class to acts as a service factory. 
 * The <code>ServiceFactory</code> has a specific <code>Service</code> 
 * connected to it.
 *
 * @author Tony
 * @author Magnus
 * @see Service
 */
public interface ServiceFactory {

    /**
     * Returns an instance of the service connected to the <code>ServiceFactory</code>.
     *
     * @return the connected service.
     */
    public Service newService();

}
