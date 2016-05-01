package cotton.services;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
*
* @author Tony 
*/
public class ServiceLookup implements ActiveServiceLookup{

  	private ConcurrentHashMap<String,ServiceMetaData> hashMap;

  	public ServiceLookup(){
  		this.hashMap = new ConcurrentHashMap<>();
  	}
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
    public boolean registerService(String serviceName, ServiceFactory serviceFactory,int maxCapacity){
      ServiceMetaData metaData = new ServiceMetaData(serviceFactory, maxCapacity);
        if(hashMap.putIfAbsent(serviceName, metaData) == null) {
            return true;    // no mapping for this key
        }
        return false;

    }

    /**
     * Returns the <code>ServiceMetaData</code> for a specified service.
     *
     * @param serviceName the specified service.
     * @return the meta data of the service.
     */
  	public ServiceMetaData getService(String serviceName){
  		return hashMap.get(serviceName);
  	}

    /**
     * Returns an <code>String Enumeration</code> of the keys in the lookup table.
     * The order of the keys will be kept from the lookup table.
     *
     * @return the keys in the lookup table.
     */
  	public Enumeration<String> getServiceEnumeration(){
  		return hashMap.keys();
  	}

    /**
     * Returns the key set in the hash map table. The order of the keys will be
     * unpredictable.
     *
     * @return the key set of the hash map.
     */
  	public ConcurrentHashMap.KeySetView<String, ServiceMetaData> getKeySet(){
  		return hashMap.keySet();
  	}

    /**
     * Removes a service entry from the lookup table.
     *
     * @param service the service name.
     * @return the meta data about the removed service.
     */
  	public ServiceMetaData removeServiceEntry(String service){
  		return hashMap.remove(service);
  	}
}
