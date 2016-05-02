package cotton.test.services;

import cotton.network.ServiceChain;
import cotton.services.*;

import java.io.InputStream;
import java.io.Serializable;
import cotton.network.DeprecatedServiceConnection;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class MathServices implements DeprecatedService{

    @Override
    public byte[] execute(CloudContext ctx, DeprecatedServiceConnection from, byte[] data, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
