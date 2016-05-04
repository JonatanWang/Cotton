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

import java.util.concurrent.CountDownLatch;

/**
 * The <code>SocketLatch</code> is designed to block the program until an incoming 
 * packet is received or set as failed. This latch is designed to work with the 
 * keep alive message.
 * 
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 * @author Magnus
 **/
public class SocketLatch{
    private NetworkPacket networkPacket = null;
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * Blocks the program until the <code>NetworkPacket</code> is set as received 
     * or failed.
     * 
     * @return returns the set <code>NetworkPacket</code>.
     */
    public NetworkPacket getData() {
        boolean loop = false;
        do {
            try {
                latch.await();
                loop = false;
            } catch (InterruptedException ex) {loop = true;}
        }while(loop);
        return networkPacket;
    }

    /**
     * Sets the <code>NetworkPacket</code> as well as executes the latch countdown.
     * 
     * @param networkPacket the <code>NetworkPacket</code> releasing the latch.
     */
    public void setData(NetworkPacket networkPacket) {
        this.networkPacket = networkPacket;
        latch.countDown();
    }
    
    /**
     * Sets the <code>NetworkPacket</code> as an packet containing the error and 
     * releases the latch.
     * 
     * @param errorMessage the <code>NetworkPacket</code> containing the error.
     */
    public void setFailed(NetworkPacket errorMessage) {
        networkPacket = errorMessage;
        latch.countDown();
    }
}
