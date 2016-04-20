package cotton.network;

/**
 *
 *
 * @author Magnus
 * @author Jonathan
 * @author Tony
 * @author Gunnlaugur
 */
public interface ServiceChain {

    public String getNextServiceName();

    public String peekNextServiceName();

    public boolean addService(String name);

    public ServiceChain into(String name);

}
