/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.cotton.mockup;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Magnus
 */
public class DefaultActiveServiceLookup implements ActiveServiceLookup{

    private ConcurrentHashMap<String,ServiceMetaData> hashMap;
    
    
    public DefaultActiveServiceLookup() {
        this.hashMap = new ConcurrentHashMap<String,ServiceMetaData>();
    }
    
    @Override
    public boolean registrateService(String serviceName, ServiceFactory serviceFactory,int maxCapacity) {
        ServiceMetaData metaData = new ServiceMetaData(serviceFactory, maxCapacity);
        hashMap.put(serviceName, metaData);
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
    public ServiceMetaData removeServiceEntry(String service) {
        return hashMap.remove(service);
    }
    
}
