package main.java.cotton.mockup;

import java.io.InputStream;
import java.io.Serializable;
/**
 *@author Tony
 *@author Magnus
 **/
public interface ServiceInstance {
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to);
}
