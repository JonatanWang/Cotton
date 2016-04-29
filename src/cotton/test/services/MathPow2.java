
package cotton.test.services;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.Service;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class MathPow2 implements Service{
    @Override
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {
        int num = new Integer(0);
        num = ByteBuffer.wrap(data).getInt();
        num = num*num;
        return ByteBuffer.allocate(4).putInt(num).array();
    }

    public static ServiceFactory getFactory() {
        return new Factory();
    }

    public static class Factory implements ServiceFactory {

        @Override
        public Service newService() {
            return new MathPow2();
        }
    }
}
