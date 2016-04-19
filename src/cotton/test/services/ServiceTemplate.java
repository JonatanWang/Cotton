
package cotton.test.services;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author Tony
 * @author Magnus
 **/
public class ServiceTemplate implements ServiceInstance {    
    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
        return "none";
    }
    
    public static ServiceFactory getFactory(){
        return new Factory();
    }
    public static class Factory implements ServiceFactory {
        @Override
        public ServiceInstance newServiceInstance() {
            return new ServiceTemplate();
        }
    }
}
