/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */

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

    @Override
    public String toString() {
        return "Origin{" +
                "address=" + address +
                ", serviceRequestID=" + serviceRequestID +
                ", socketLatchID=" + socketLatchID +
                '}';
    }
}
