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
import java.util.ArrayList;

/**
 *
 * @author magnus
 */
public class CloudSnapshot implements Serializable {

    private DiscoveryProbe[] snapshot;
    /**
     * Used to send one address per service,
     * @param snapshot Array of services and request queues, one each
     */
    public CloudSnapshot(DiscoveryProbe[] snapshot) {
        if (snapshot == null) {
            this.snapshot = new DiscoveryProbe[0];
        } else {
            this.snapshot = snapshot;
        }
    }

    /**
     * Used to send one address per service,
     * @param snapshot Array of services and request queues, one each
     */
    public CloudSnapshot(ArrayList<DiscoveryProbe> snapshot) {
        if (snapshot == null) {
            this.snapshot = new DiscoveryProbe[0];
        } else {
            this.snapshot = snapshot.toArray(new DiscoveryProbe[snapshot.size()]);
        }
    }
    
    /**
     * 
     * @return the current snapshot of the cloud
     */
    public DiscoveryProbe[] getSnapshot() {
        return snapshot;
    }

}
