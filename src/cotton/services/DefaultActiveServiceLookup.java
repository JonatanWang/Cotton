package cotton.services;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
 *
 *
 * @author Magnus
 */
public class DefaultActiveServiceLookup implements DeprecatedActiveServiceLookup{

    private ConcurrentHashMap<String,DeprecatedServiceMetaData> hashMap;

    public DefaultActiveServiceLookup() {
        this.hashMap = new ConcurrentHashMap<String,DeprecatedServiceMetaData>();
    }

    @Override
    public boolean registerService(String serviceName, DeprecatedServiceFactory serviceFactory,int maxCapacity) {
        DeprecatedServiceMetaData metaData = new DeprecatedServiceMetaData(serviceFactory, maxCapacity);
        if(hashMap.putIfAbsent(serviceName, metaData) == null) {
            return true;    // no mapping for this key
        }
        return false;
    }

    @Override
    public DeprecatedServiceMetaData getService(String serviceName) {
        return hashMap.get(serviceName);
    }

    @Override
    public Enumeration<String> getServiceEnumeration() {
        return hashMap.keys();
    }

    @Override
    public KeySetView<String, DeprecatedServiceMetaData> getKeySet() {
        return hashMap.keySet();
    }
    
    @Override
    public DeprecatedServiceMetaData removeServiceEntry(String service) {
        return hashMap.remove(service);
    }

}
