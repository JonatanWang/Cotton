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

import cotton.network.DestinationMetaData;
import java.io.Serializable;
import java.net.SocketAddress;

/**
 * The <code>DiscoveryProbe</code> acts as a discovery request as well as a
 * discovery response. The packet consists of a service name and a 
 * <code>SocketAddress</code> containing the targeted service address. 
 * 
 * @author Magnus
 */
public class DiscoveryProbe implements Serializable {
    private String name;
    private DestinationMetaData address;

    /**
     * Constructs a <code>DiscoveryPack</code> through the in parameters.
     * 
     * @param name the service name.
     * @param address the service address.
     */
    public DiscoveryProbe(String name, DestinationMetaData address) {
        this.name = name;
        this.address = address;
    }

    /**
     * Returns the service name contained in the packet.
     * 
     * @return the service name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the service name in the packet.
     * 
     * @param name the service name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the <code>SocketAddress</code> contained in the packet. This address
     * points to the service location.
     * 
     * @return the <code>SocketAddress</code>
     */
    public DestinationMetaData getAddress() {
        return address;
    }

    /**
     * Sets the <code>SocketAddress</code> in the packet. This address should 
     * point to the service location.
     * 
     * @param address the new address.
     */
    public void setAddress(DestinationMetaData address) {
        this.address = address;
    }        
}
