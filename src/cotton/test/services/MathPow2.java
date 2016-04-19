
package cotton.test.services;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class MathPow2 implements ServiceInstance {
    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
        Integer num = new Integer(0);
        try {
            ObjectInputStream input = new ObjectInputStream(data);
            num =  (Integer)input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(MathPow2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MathPow2.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            int value = num.intValue();
            num = new Integer(value * value);
        }        
        return num;
    }

    public static ServiceFactory getFactory() {
        return new Factory();
    }

    public static class Factory implements ServiceFactory {

        @Override
        public ServiceInstance newServiceInstance() {
            return new MathPow2();
        }
    }
}
