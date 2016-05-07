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

import cotton.network.PathType;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 *
 * @author Tony
 * @author Magnus
 */
public class ConfigurationPacket implements Serializable {

    private SocketAddress instanceAddress;
    private int maxTotalRequestQueues;
    private int maxTotalServices;
    private boolean globalServiceDiscovery;
    private ConfigEntry[] entries;
    
    public ConfigurationPacket(SocketAddress instanceAddress, int maxTotalRequestQueues, int maxTotalServices, boolean globalServiceDiscovery) {
        this.instanceAddress = instanceAddress;
        this.maxTotalRequestQueues = maxTotalRequestQueues;
        this.maxTotalServices = maxTotalServices;
        this.globalServiceDiscovery = globalServiceDiscovery;
        this.entries = new ConfigEntry[0];
    }
    
    public SocketAddress getInstanceAddress() {
        return instanceAddress;
    }

    public void setInstanceAddress(SocketAddress instanceAddress) {
        this.instanceAddress = instanceAddress;
    }

    public int getMaxTotalRequestQueues() {
        return maxTotalRequestQueues;
    }

    public void setMaxTotalRequestQueues(int maxTotalRequestQueues) {
        this.maxTotalRequestQueues = maxTotalRequestQueues;
    }

    public int getMaxTotalServices() {
        return maxTotalServices;
    }

    public ConfigEntry[] getConfigEntry(){
        return this.entries;
    }
    
    public void setMaxTotalServices(int maxTotalServices) {
        this.maxTotalServices = maxTotalServices;
    }

    public boolean isGlobalServiceDiscovery() {
        return globalServiceDiscovery;
    }

    public void setGlobalServiceDiscovery(boolean globalServiceDiscovery) {
        this.globalServiceDiscovery = globalServiceDiscovery;
    }
    
    public void setConfigEntry(ConfigEntry[] entries){
        this.entries = entries;
    }
}
