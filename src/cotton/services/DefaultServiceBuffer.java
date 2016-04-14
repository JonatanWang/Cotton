package cotton.services;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
public class DefaultServiceBuffer implements ServiceBuffer{

    private ConcurrentLinkedQueue<ServicePacket> buffer;

    public DefaultServiceBuffer(){
        buffer = new ConcurrentLinkedQueue<>();
    }

    public ServicePacket nextPacket(){
        return buffer.poll();
    }

    public boolean add(ServicePacket servicePacket){
        return buffer.add(servicePacket);
    }

}
