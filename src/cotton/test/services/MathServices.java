package test.java.cotton.services;

import java.io.InputStream;
import java.io.Serializable;
import main.java.cotton.mockup.CloudContext;
import main.java.cotton.mockup.ServiceChain;
import main.java.cotton.mockup.ServiceConnection;
import main.java.cotton.mockup.ServiceInstance;

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
