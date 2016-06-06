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

package cotton.internalrouting;

import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.servicediscovery.RouteSignal;


/**
 *
 * @author tony
 */
public interface InternalRoutingServiceDiscovery {
    /**
     * Sends information back to origin.
     * @param origin
     * @param pathType
     * @param data
     * @return 
     */
    public boolean sendBackToOrigin(Origin origin,PathType pathType,byte[] data);
    /**
     * Sends data to a given destination. 
     * @param dest
     * @param data
     * @return 
     */
    public boolean sendToDestination(DestinationMetaData dest,byte[] data);
    /**
     * Sends data to a given destination and use serviceChain. 
     * @param dest
     * @param serviceChain
     * @param data
     * @return 
     */
    public boolean sendToDestination(DestinationMetaData dest,ServiceChain serviceChain,byte[] data);
    /**
     * sends data to a destination and awaits response and also sets a timestamp to the service request.
     * @param dest
     * @param data
     * @param timeout
     * @return 
     */
    public ServiceRequest sendWithResponse(DestinationMetaData dest, byte[] data, int timeout);
    /**
     * notifies the request queue of the service discovery
     * @param destination
     * @param route
     * @param serviceName
     * @return 
     */
    public boolean notifyRequestQueue(DestinationMetaData destination,RouteSignal route, String serviceName);
    /**
     * Sends data locally.
     * @param destination
     * @param route
     * @param data
     * @return 
     */
    public boolean sendLocal(DestinationMetaData destination,RouteSignal route,byte[] data);
    
}
