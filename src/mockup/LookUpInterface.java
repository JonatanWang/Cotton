package mockup;
import java.util.Enumeration;

public interface LookUpInterface {
        public boolean registrateService(String serviceName, ServiceFactory serviceFactory);
        public ServiceMetaData getService(String serviceName);
        public Enumeration<String> getServiceEnumeration();
        public boolean removeServiceEntry(String service);

}
