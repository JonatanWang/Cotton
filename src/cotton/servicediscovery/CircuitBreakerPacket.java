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


package cotton.servicediscovery;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * The <code>CircuitBreakerPacket</code> wraps the service list and <code>SocketAddress</code> 
 * of a <code>Cotton</code> instance.
 * 
 * @author Magnus
 * @see SocketAddress
 */
public class CircuitBreakerPacket implements Serializable {
    private SocketAddress instanceAddress;
    private String circuitName;
    /**
     * Constructs a <code>CircuitBreakerPacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     */
    public CircuitBreakerPacket(String circuitName) {
        this.instanceAddress = null;
        this.circuitName = circuitName;
    }
    /**
     * Constructs a <code>CircuitBreakerPacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     * 
     * @param instanceAddress the <code>Cotton</code> instance address.
     */
    public CircuitBreakerPacket(SocketAddress instanceAddress, String circuitName) {
        this.instanceAddress = instanceAddress;
        this.circuitName = circuitName;
    }

    /**
     * Returns the containing <code>SocketAddress</code> of the 
     * <code>CircuitBreakerPacket</code>.
     * 
     * @return the containing <code>SocketAddress</code>.
     */
    public SocketAddress getInstanceAddress() {
        return instanceAddress;
    }

    /**
     * Returns the containing <code>circuitName</code> of the 
     * <code>CircuitBreakerPacket</code>.
     * 
     * @return the containing <code>circuitName</code>.
     */
    public String getCircuitName() {
        return circuitName;
    }
    
    public void setInstanceAddress(SocketAddress instanceAddress){
        this.instanceAddress = instanceAddress;
    }

    @Override
    public String toString() {
        return "CircuitBreakerPacket{" +
                "instanceAddress=" + instanceAddress +
                ", circuitName='" + circuitName + '\'' +
                '}';
    }
}
