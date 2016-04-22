package cotton.services;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
 *
 *
 * @author Magnus
 */
public class DefaultActiveServiceLookup implements ActiveServiceLookup{

    private ConcurrentHashMap<String,ServiceMetaData> hashMap;

    public DefaultActiveServiceLookup() {
        this.hashMap = new ConcurrentHashMap<String,ServiceMetaData>();
    }

    @Override
    public boolean registerService(String serviceName, ServiceFactory serviceFactory,int maxCapacity) {
        ServiceMetaData metaData = new ServiceMetaData(serviceFactory, maxCapacity);
        if(hashMap.putIfAbsent(serviceName, metaData) == null) {
            return true;    // no mapping for this key
        }
        return false;
    }

    @Override
    public ServiceMetaData getService(String serviceName) {
        return hashMap.get(serviceName);
    }

    @Override
    public Enumeration<String> getServiceEnumeration() {
        return hashMap.keys();
    }

    @Override
    public KeySetView<String, ServiceMetaData> getKeySet() {
        return hashMap.keySet();
    }
    
    @Override
    public ServiceMetaData removeServiceEntry(String service) {
        return hashMap.remove(service);
    }

}
