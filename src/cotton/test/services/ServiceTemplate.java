
package cotton.test.services;

import cotton.services.Service;
import cotton.services.ServiceFactory;
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
import cotton.services.ActiveServiceLookup;
import java.io.IOException;

/**
 *
 * @author Tony
 * @author Magnus
 **/
public class ServiceTemplate implements Service {
    @Override
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {
        return "none".getBytes();
    }
    
    public static ServiceFactory getFactory(){
        return new Factory();
    }
    public static class Factory implements ServiceFactory {
        @Override
        public Service newService() {
            return new ServiceTemplate();
        }
    }
}
