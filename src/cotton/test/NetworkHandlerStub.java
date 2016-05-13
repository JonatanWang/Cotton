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


package cotton.test;

import cotton.network.NetworkHandler;
import cotton.internalrouting.InternalRoutingNetwork;
import cotton.network.SocketLatch;
import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import java.io.IOException;

/**
*
* @author Tony
*/
public class NetworkHandlerStub implements NetworkHandler{
    private InternalRoutingNetwork internal;
    private SocketAddress addr;
    
    public NetworkHandlerStub(SocketAddress addr){
        this.addr = addr;
        this.internal = null;
    }
    
    /**
     * Sends data wrapped in a <code>NetworkPacket</code> over the network.
     *
     * @param netPacket contains the data and the <code>metadata</code> needed to send the packet.
     * @param addr defines the <code>SocketAddress</code> to send through.
     */
    @Override
    public void send(NetworkPacket netPacket, SocketAddress addr) throws IOException{
        internal.pushNetworkPacket(netPacket);
    }

    /**
     * Sends a <code>NetworkPacket</code> and informs that the connection should stay alive.
     *
     * @param netPacket wraps the keep alive flag.
     * @param addr defines the <code>SocketAddress</code> to send through.
     */
    @Override
    public void sendKeepAlive(NetworkPacket netPacket,SocketAddress addr) throws IOException{
        SocketLatch latch = new SocketLatch();
        internal.pushKeepAlivePacket(netPacket,latch);
    }

    /**
     * Returns the local <code>SocketAddress</code> of the running machine.
     *
     * @return the local <code>SocketAddress</code>.
     */
    @Override
    public SocketAddress getLocalAddress(){
        return addr;
    }

    /**
     * Sets the interface to push data to the rest of the system
     * @param internal this machines routing subsystem
     */
    @Override
    public void setInternalRouting(InternalRoutingNetwork internal){
        this.internal = internal;
    }
    
    /**
     * Asks all connections to shutdown and turns off the <code>NetworkHandler</code>.
     */
    @Override
     public void stop(){}

    @Override
    public void run(){}

}
