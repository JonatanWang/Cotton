package mockup;

import java.util.Objects;

/**
 * Created by o_0 on 2016-04-07.
 */
public interface ServiceInstance {
    public void consumeServiceOrder(ServiceConnection from, Objects data,ServiceChain to);
}
