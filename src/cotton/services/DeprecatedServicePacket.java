package cotton.services;

import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.io.Serializable;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
public class DeprecatedServicePacket implements Serializable{
    private byte[] data;
    private ServiceConnection from;
    private ServiceChain to;

    public DeprecatedServicePacket(ServiceConnection from, byte[] data, ServiceChain to) {
        this.data = data;
        this.from = from;
        this.to = to;
    }

    public byte[] getData() {
        return data;
    }

    public ServiceConnection getFrom() {
        return from;
    }

    public ServiceChain getTo() {
        return to;
    }

}
