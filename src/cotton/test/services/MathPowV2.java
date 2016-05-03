
package cotton.test.services;

import cotton.network.Origin;
import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.services.Service;
import cotton.services.ServiceFactory;

/**
 *
 * @author Magnus
 * @author Tony
 */
public class MathPowV2 implements Service{

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
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
            return new MathPowV2();
        }
    }
}
