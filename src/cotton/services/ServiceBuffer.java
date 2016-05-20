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


package cotton.services;

import cotton.network.NetworkPacket;

/**
 * A <code>ServiceBuffer</code> stores incoming <code>ServicePackets</code> and 
 * distributes packets.
 *
 * @author Tony Tran
 * @see ServicePacket
 **/
public interface ServiceBuffer{

    /**
     * Distributes the next packet in the <code>ServiceBuffer</code>.
     * 
     * @return the next <code>ServicePacket</code> in the buffer.
     */
    public NetworkPacket nextPacket();
    
    /**
     * Stores a <code>ServicePacket</code> in the buffer.
     * 
     * @param servicePacket the <code>ServicePacket</code> to store.
     * @return <code>true</code> if the buffer changed as a result of the <code>add</code>.
     */
    public boolean add(cotton.network.NetworkPacket servicePacket);
    /**
     * Returns the current size,
     * @return number of items currently in buffer
     */
    public int size();
}
