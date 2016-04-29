package cotton.services;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
public class DefaultServiceBuffer implements DeprecatedServiceBuffer{

    private ConcurrentLinkedQueue<DeprecatedServicePacket> buffer;

    public DefaultServiceBuffer(){
        buffer = new ConcurrentLinkedQueue<>();
    }

    public DeprecatedServicePacket nextPacket(){
        return buffer.poll();
    }

    public boolean add(DeprecatedServicePacket servicePacket){
        return buffer.add(servicePacket);
    }

}
