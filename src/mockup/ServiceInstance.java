package mockup;

import java.io.InputStream;
import java.io.Serializable;
/**
 * Created by o_0 on 2016-04-07.
 */
public interface ServiceInstance {
    public Serializable consumeServiceOrder(ServiceConnection from, InputStream data,ServiceChain to);
}
