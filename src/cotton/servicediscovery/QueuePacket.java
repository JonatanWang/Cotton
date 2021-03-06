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
 * The <code>AnnouncePacket</code> wraps the service list and <code>SocketAddress</code> 
 * of a <code>Cotton</code> instance.
 * 
 * @author Magnus
 * @see SocketAddress
 */
public class QueuePacket implements Serializable {
    private SocketAddress instanceAddress;
    private String[] requestQueueList;
    private boolean shouldRemove = false;
    /**
     * Constructs a <code>AnnouncePacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     * 
     * @param instanceAddress the <code>Cotton</code> instance address.
     */
    public QueuePacket(SocketAddress instanceAddress,String name) {
        this.instanceAddress = instanceAddress;
        String[] serviceName = new String[]{name};
        this.requestQueueList = serviceName;
        
    }

    /**
     * True indicates that the listed queues should be removed instead of added
     * @return 
     */
    public boolean isShouldRemove() {
        return shouldRemove;
    }

    /**
     *  True indicates that the listed queues should be removed instead of added
     * @param shouldRemove set true if the listed queue should be removed, it defaults to false
     */
    public void setShouldRemove(boolean shouldRemove) {
        this.shouldRemove = shouldRemove;
    }
    
    
    
    /**
     * Constructs a <code>AnnouncePacket</code> containing the current <code>Cotton</code> 
     * instance <code>SocketAddress</code> and the service list.
     * 
     * @param instanceAddress the <code>Cotton</code> instance address.
     * @param serviceList the <code>Cotton</code> instance service list.
     */
    public QueuePacket(SocketAddress instanceAddress, String[] serviceList) {
        this.instanceAddress = instanceAddress;
        this.requestQueueList = serviceList;
    }

    /**
     * Returns the containing <code>SocketAddress</code> of the 
     * <code>AnnouncePacket</code>.
     * 
     * @return the containing <code>SocketAddress</code>.
     */
    public SocketAddress getInstanceAddress() {
        return instanceAddress;
    }

    /**
     * Returns the containing <code>serviceList</code> of the 
     * <code>AnnouncePacket</code>.
     * 
     * @return the containing <code>serviceList</code>.
     */
    public String[] getRequestQueueList() {
        return requestQueueList;
    }
}
