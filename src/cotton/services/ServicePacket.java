package cotton.services;

import cotton.network.Origin;
import cotton.network.ServiceChain;
import java.io.Serializable;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
public class ServicePacket implements Serializable{
    private byte[] data;
    private Origin origin;
    private ServiceChain to;

    public ServicePacket(Origin origin, byte[] data, ServiceChain to) {
        this.data = data;
        this.origin = origin;
        this.to = to;
    }

    public byte[] getData() {
        return data;
    }

    public Origin getOrigin() {
        return origin;
    }

    public ServiceChain getTo() {
        return to;
    }

}
