package mockup;
import java.util.Enumeration;
/**
 *@author Tony
 *@author Magnus
 **/
public interface ActiveServiceLookup {
        public boolean registrateService(String serviceName, ServiceFactory serviceFactory,int maxCapacity);
        public ServiceMetaData getService(String serviceName);
        public Enumeration<String> getServiceEnumeration();
        public ServiceMetaData removeServiceEntry(String service);
}