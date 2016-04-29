package cotton.network;

import java.net.SocketAddress;
import java.util.UUID;

/**
 *
 * @author Tony
 * @author Magnus
 */
public class Origin {

    private SocketAddress address;
    private UUID serviceRequestID;
    private UUID socketLatchID;

    public Origin() {
        this.address = null;
        this.serviceRequestID = null;
        this.socketLatchID = null;
    }

    public Origin(SocketAddress address, UUID serviceRequestID) {
        this.address = address;
        this.serviceRequestID = serviceRequestID;
        this.socketLatchID = null;
    }

    /**
     * Returns value of address
     *
     * @return
     */
    public SocketAddress getAddress() {
        return this.address;
    }

    /**
     * Returns value of serviceRequestID
     *
     * @return
     */
    public UUID getServiceRequestID() {
        return this.serviceRequestID;
    }

    /**
     * Returns value of socketLatchID
     *
     * @return
     */
    public UUID getSocketLatchID() {
        return this.socketLatchID;
    }

    /**
     * Sets new value of address
     *
     * @param address
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }
    
    /**
     * Sets new value of requestID
     * @param serviceRequestID 
     */
    public void setServiceRequestID(UUID serviceRequestID){
      this.serviceRequestID = serviceRequestID;
    }
    /**
     * Sets new value of socketLatchID
     *
     * @param socketLatchID
     */
    public void setSocketLatchID(UUID socketLatchID) {
        this.socketLatchID = socketLatchID;
    }

}
