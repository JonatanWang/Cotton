package cotton.test.services;

import cotton.services.*;

import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class MathServices implements ServiceInstance {

    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
