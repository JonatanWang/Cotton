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
public interface DeprecatedActiveServiceLookup {

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
    public boolean registerService(String serviceName, DeprecatedServiceFactory serviceFactory,int maxCapacity);

    /**
     * Returns the <code>ServiceMetaData</code> for a specified service.
     * 
     * @param serviceName the specified service.
     * @return the meta data of the service.
     */
    public DeprecatedServiceMetaData getService(String serviceName);
    
    /**
     * Returns an <code>String Enumeration</code> of the keys in the lookup table.
     * The order of the keys will be kept from the lookup table.
     * 
     * @return the keys in the lookup table.
     */
    public Enumeration<String> getServiceEnumeration();
    
    /**
     * Returns the key set in the hash map table. The order of the keys will be
     * unpredictable.
     * 
     * @return the key set of the hash map.
     */
    public ConcurrentHashMap.KeySetView<String, DeprecatedServiceMetaData> getKeySet();

    /**
     * Removes a service entry from the lookup table.
     * 
     * @param service the service name.
     * @return the meta data about the removed service.
     */
    public DeprecatedServiceMetaData removeServiceEntry(String service);
}
