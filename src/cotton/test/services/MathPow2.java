
package cotton.test.services;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.services.DeprecatedService;
import cotton.services.DeprecatedServiceFactory;

/**
 *
 * @author Magnus
 */
public class MathPow2 implements DeprecatedService{
    @Override
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {
        int num = new Integer(0);
        num = ByteBuffer.wrap(data).getInt();
        num = num*num;
        return ByteBuffer.allocate(4).putInt(num).array();
    }

    public static DeprecatedServiceFactory getFactory() {
        return new Factory();
    }

    public static class Factory implements DeprecatedServiceFactory {

        @Override
        public DeprecatedService newService() {
            return new MathPow2();
        }
    }
}
