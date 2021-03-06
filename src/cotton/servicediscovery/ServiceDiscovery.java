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

import cotton.internalrouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.services.ActiveServiceLookup;
import cotton.systemsupport.Command;
import cotton.systemsupport.StatisticsProvider;
import java.net.SocketAddress;

/**
 *
 * @author Magnus
 */
public interface ServiceDiscovery extends StatisticsProvider {
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress);
    public void setLocalServiceTable(ActiveServiceLookup serviceTable);
    public RouteSignal getDestination(DestinationMetaData destination, Origin origin, ServiceChain to); // outgoinging package
    public RouteSignal getLocalInterface(Origin origin, ServiceChain to); // incoming packaged
    public boolean announce();
    public void stop();
    public void discoveryUpdate(Origin origin, byte[] data);
    public void requestQueueMessage(DiscoveryPacket packet);
    public RouteSignal getRequestQueueDestination(DestinationMetaData destination, String serviceName);
    public boolean announceQueues(RequestQueueManager queueManager);
    public DestinationMetaData destinationUnreachable(DestinationMetaData dest,String serviceName);
    public DestinationMetaData getDestinationForType(PathType type,String name);
}
