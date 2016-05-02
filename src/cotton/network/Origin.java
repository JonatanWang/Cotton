package cotton.network;

import java.net.SocketAddress;
import java.util.UUID;

/**
 * Contains connection information about the origin of a service request. The 
 * <code>Origin class</code> also contains the latch id for the request.
 * 
 * @author Tony
 * @author Magnus
 */
public class Origin {
    private SocketAddress address;
    private UUID serviceRequestID;
    private UUID socketLatchID;

    /**
     * Constructs an empty <code>Origin</code> connection.
     */
    public Origin() {
        this.address = null;
        this.serviceRequestID = null;
        this.socketLatchID = null;
    }

    /**
     * Constructs an <code>Origin</code> connection based on incoming parameter.
     * 
     * @param address the <code>ServiceRequest</code> address.
     * @param serviceRequestID the service request id.
     */
    public Origin(SocketAddress address, UUID serviceRequestID) {
        this.address = address;
        this.serviceRequestID = serviceRequestID;
        this.socketLatchID = null;
    }

    /**
     * Returns the address value.
     *
     * @return the address.
     */
    public SocketAddress getAddress() {
        return this.address;
    }

    /**
     * Returns the serviceRequestID value.
     *
     * @return the serviceRequestID.
     */
    public UUID getServiceRequestID() {
        return this.serviceRequestID;
    }

    /**
     * Returns the socketLatchID value.
     *
     * @return the socketLatchID.
     */
    public UUID getSocketLatchID() {
        return this.socketLatchID;
    }

    /**
     * Sets new value of address.
     *
     * @param address the new value of address.
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }
    
    /**
     * Sets new value of requestID.
     * @param serviceRequestID the new value of requestID.
     */
    public void setServiceRequestID(UUID serviceRequestID){
      this.serviceRequestID = serviceRequestID;
    }
    
    /**
     * Sets new value of socketLatchID.
     *
     * @param socketLatchID the new value of socketLatchID.
     */
    public void setSocketLatchID(UUID socketLatchID) {
        this.socketLatchID = socketLatchID;
    }
}
