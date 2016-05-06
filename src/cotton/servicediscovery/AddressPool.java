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
import java.util.ArrayList;

/**
 * The <code>AddressPool</code> acts as a list of <code>SocketAddresses</code> 
 * that you can add and remove from. The pool is initialized with zero 
 * <code>SocketAddresses</code>.
 * 
 * @author Magnus, Mats
 * @see SocketAddress
 */
public class AddressPool {

    private int pos = 0;
    private ArrayList<DestinationMetaData> pool = new ArrayList<>();

    /**
     * Adds a <code>SocketAddress</code> to the <code>AddressPool</code> concurrently.
     * 
     * @param address the <code>SocketAddress</code> to add to the <code>AddressPool</code>.
     * @return <code>true</code> always.
     */
    public boolean addAddress(DestinationMetaData address) {
        synchronized (this) {
            pool.add(address);
        }
        return true;
    }

    public DestinationMetaData[] copyPoolData() {
        DestinationMetaData[] data = null;
         synchronized (this) {
            data = pool.toArray(new DestinationMetaData[pool.size()]);
        }
        return data;
    }
    
    /**
     * removes an address from the addresspool
     * @param dest The address of the invalid destination.
     */
    public boolean remove(DestinationMetaData dest) {
        boolean flag = false;
        synchronized (this) {
            flag = pool.remove(dest);
        }
        return flag;
    }
    /**
     * Returns the next <code>SocketAddress</code> in the <code>AddressPool</code>. 
     * If the <code>AddressPool</code> is empty the function will reply <code>null</code>.
     * 
     * @return the next <code>SocketAddress</code> or null if the list is empty.
     */
    public DestinationMetaData getAddress() {
        DestinationMetaData addr = null;
        
        synchronized (this) {
            if (pool.isEmpty() == false) {
                pos = pos % pool.size();
                addr = pool.get(pos);
                pos++;
            }
        }
        return addr;
    }
}
