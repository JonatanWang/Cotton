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
import java.net.SocketAddress;
/**
 * @author Tony
 * @author Magnus
 */
public class TopologyPacket {
    private DestinationMetaData instanceAddress;
  	private SocketAddress originalSource;
  	private SocketAddress lastJump;
    private boolean isGlobalDiscovery;
    private int count;
    /**
     * Constructs a <code>AnnouncePacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     * 
     * @param instanceAddress the <code>Cotton</code> instance address.
     */
    public TopologyPacket(DestinationMetaData instanceAddress,SocketAddress originalSource,int count) {
        this.instanceAddress = instanceAddress;
      	this.originalSource = originalSource;
      	this.lastJump = originalSource;
        this.count = count;
    }
    
    /**
     * Returns the containing <code>DestinationMetaData</code> of the 
     * <code>TopologyPacket</code>.
     * 
     * @return the containing <code>DestinationMetaData</code>.
     */
    public DestinationMetaData getInstanceAddress() {
        return instanceAddress;
    }
    /**
     * decrements a value 
     * @return count the value after decrement 
     */
  	public int decrementCount(){
        count--;
        return count;
  	}

    /**
     * returns a value
     * @return count a intger that represent how many connections that should be connected in next topology jump
     */
  	public int getCount(){
        return this.count;
    }

    /**
     * returns the address of the instance that initiated a topology change
     * @return socketAddress the address of the instance that initiated the topology change
     */
  	public SocketAddress getOriginalSource(){
  		return originalSource;
  	}

    /**
     * checks whether the instance is a global discovery or not.
     *
     */
    public boolean isGlobalDiscovery(){
        return isGlobalDiscovery;
    }

    /**
     * sets whether the instance is a global discovery or not.
     * @param globalDiscovery a boolean that indicates if the instance is a global discovery
     */
    public void setGlobalDiscovery(boolean globalDiscovery){
        this.isGlobalDiscovery = globalDiscovery;
    }

    /**
     * returns the last hop address in order to prevent recursive loops in topology
     * @return lastJump the address of the last hop address 
     */
  	public SocketAddress getLastJump(){
        return lastJump;
  	}

    /**
     * sets last jump address.
     * @param lastJump the lastJump address for the topology change
     */
  	public void setLastJump(SocketAddress lastJump){
        this.lastJump = lastJump;
  	}
}
