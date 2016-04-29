
package cotton.test.services;

import cotton.services.CloudContext;
import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.Serializable;
import cotton.Cotton;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import cotton.services.DeprecatedService;
import cotton.services.DeprecatedServiceFactory;
import cotton.services.DeprecatedActiveServiceLookup;

/**
 *
 * @author Tony
 * @author Magnus
 **/
public class ServiceTemplate implements DeprecatedService {
    @Override
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {
        return "none".getBytes();
    }
    
    public static DeprecatedServiceFactory getFactory(){
        return new Factory();
    }
    public static class Factory implements DeprecatedServiceFactory {
        @Override
        public DeprecatedService newService() {
            return new ServiceTemplate();
        }
    }
}
