package cotton.services;

/**
 *
 *
 * @author Magnus
 */
public interface ServiceChain {

    public String getNextServiceName();    // temporary dummy interface get service name

    public String getCurrentServiceName(); // temporary dummy interface get service name

    public boolean addService(String name); // add temporary service name
    public ServiceChain into(String name);
}
