package cotton.test.services;

import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.CloudContext;
import cotton.services.Service;

import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class MathServices implements Service{

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
