package cotton.services;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author Tony
 * @author Magnus
 **/
public interface ActiveServiceLookup {

    public boolean registerService(String serviceName, ServiceFactory serviceFactory,int maxCapacity);

    public ServiceMetaData getService(String serviceName);

    public Enumeration<String> getServiceEnumeration();
    public ConcurrentHashMap.KeySetView<String, ServiceMetaData> getKeySet();

    public ServiceMetaData removeServiceEntry(String service);

}
