package cotton.services;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The <code>ActiveServiceLookup</code> acts as a lookup table for services. The
 * class implementing the interface is intended to be used by the 
 * <code>ServiceHandler</code> to register, get services as well as remove services 
 * from the lookup table.
 * 
 * @author Tony
 * @author Magnus
 * @see ServiceHandler
 **/
public interface ActiveServiceLookup {

    /**
     * Registers a service to the lookup table. To register a service the user
     * has to define the service name, the factory used to make instances of the service
     * and the maximum capacity of the service instances.
     * 
     * @param serviceName defines what the service is called.
     * @param serviceFactory the factory used to make instances of the service.
     * @param maxCapacity defines how many instances of the service are allowed.
     * @return whether the registration was successful or not.
     */
    public boolean registerService(String serviceName, ServiceFactory serviceFactory,int maxCapacity);

    /**
     * Returns the <code>ServiceMetaData</code> for a specified service.
     * 
     * @param serviceName the specified service.
     * @return the meta data of the service.
     */
    public ServiceMetaData getService(String serviceName);
    public Enumeration<String> getServiceEnumeration();
    public ConcurrentHashMap.KeySetView<String, ServiceMetaData> getKeySet();

    public ServiceMetaData removeServiceEntry(String service);

}
