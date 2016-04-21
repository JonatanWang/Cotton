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
public class ServicePacket implements Serializable{
    private InputStream dataStream;
    private ServiceConnection from;
    private ServiceChain to;

    public ServicePacket(ServiceConnection from,InputStream data,ServiceChain to) {
        this.dataStream = data;
        this.from = from;
        this.to = to;
    }

    public InputStream getDataStream() {
        return dataStream;
    }

    public ServiceConnection getFrom() {
        return from;
    }

    public ServiceChain getTo() {
        return to;
    }

}
