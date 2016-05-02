package cotton.network;

/**
 * The <code>ServiceChain</code> maintains the chain of services to be executed. 
 *
 * @author Magnus
 * @author Jonathan
 * @author Tony
 * @author Gunnlaugur
 */
public interface ServiceChain {
    /**
     * Removes the service name and returns it from the queue.
     * 
     * @return the next service name.
     */
    public String getNextServiceName();
    /**
     * Peeks at the next service name in the queue and returns it. The service 
     * will not be removed from the queue.
     * 
     * @return the next service name.
     */
    public String peekNextServiceName();
    /**
     * Adds a service to the service queue.
     * 
     * @param name the name of the service.
     * @return <code>true</code> if the service is added.
     */
    public boolean addService(String name);
    /**
     * Adds a service to the service queue and returns this object. This allows 
     * more than one add in a single command.
     * 
     * @param name the service to add to the queue.
     * @return <code>this</code> object.
     */
    public ServiceChain into(String name);
}
