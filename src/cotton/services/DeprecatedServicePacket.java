package cotton.services;

import cotton.network.ServiceChain;
import java.io.InputStream;
import java.io.Serializable;
import cotton.network.DeprecatedServiceConnection;

/**
 *
 *
 * @author Magnus
 * @author Tony
 */
@Deprecated
public class DeprecatedServicePacket implements Serializable{
    private byte[] data;
    private DeprecatedServiceConnection from;
    private ServiceChain to;

    public DeprecatedServicePacket(DeprecatedServiceConnection from, byte[] data, ServiceChain to) {
        this.data = data;
        this.from = from;
        this.to = to;
    }

    public byte[] getData() {
        return data;
    }

    public DeprecatedServiceConnection getFrom() {
        return from;
    }

    public ServiceChain getTo() {
        return to;
    }

}
