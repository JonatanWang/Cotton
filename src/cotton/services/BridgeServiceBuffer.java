package cotton.services;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
public class BridgeServiceBuffer implements ServiceBuffer{

    private ConcurrentLinkedQueue<ServicePacket> buffer;

    public BridgeServiceBuffer(){
        buffer = new ConcurrentLinkedQueue<>();
    }

    public ServicePacket nextPacket(){
        return buffer.poll();
    }

    public boolean add(ServicePacket servicePacket){
        return buffer.add(servicePacket);
    }

}
