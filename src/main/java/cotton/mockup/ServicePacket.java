
package main.java.cotton.mockup;

import java.io.InputStream;

/**
 *
 * @author Magnus
 *@author Tony
 */
public class ServicePacket{
    private InputStream dataStream;
    private ServiceConnection from;
    private ServiceChain to;
    public ServicePacket(ServiceConnection from,ServiceChain to,InputStream data) {
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
