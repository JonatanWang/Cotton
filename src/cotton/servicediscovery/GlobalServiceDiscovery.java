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

import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import cotton.services.ActiveServiceLookup;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import cotton.services.ServiceMetaData;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
/**
 *
 * @author magnus
 * @author Tony
 */
public class GlobalServiceDiscovery implements ServiceDiscovery{

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;
    private AddressPool discoveryCache;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private ActiveServiceLookup localServiceTable = null;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String,AddressPool> activeQueue;
    private ConcurrentHashMap<DestinationMetaData,AtomicInteger> destFailStat = new ConcurrentHashMap();
    
    
    /**
     * Fills in a list of all pre set globalServiceDiscovery addresses
     * @param globalDNS information from the config file
     */
    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        if (globalDNS != null) {
            SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
            for (int i = 0; i < addrArr.length; i++) {
                DestinationMetaData gAddr = new DestinationMetaData(addrArr[i],PathType.DISCOVERY); 
                discoveryCache.addAddress(gAddr);
            }
        }
    }

    public GlobalServiceDiscovery(GlobalDiscoveryDNS dnsConfig) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(dnsConfig);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        threadPool = Executors.newCachedThreadPool();
        this.activeQueue = new ConcurrentHashMap<>();
    }

    /**
     * So we can get out to other machines
     * @param network GlobalServiceDiscovery way out to the world
     * @param localAddress what machine its one and its port
     */
    @Override
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress) {
        this.internalRouting = network;
        this.localAddress = localAddress;
    }

    /**
     * If this machine runs local services set the lookup table
     * @param serviceTable a table with all the local services that can be run on this machine
     */
    @Override
    public void setLocalServiceTable(ActiveServiceLookup serviceTable) {
        this.localServiceTable = serviceTable;
    }

    
    private DestinationMetaData destinationRemove(DestinationMetaData dest, String serviceName) {
        if (serviceName != null) {
            AddressPool pool = serviceCache.get(serviceName);
            if (pool != null) {
                pool.remove(dest);
                return pool.getAddress();
            }
        }
        if (dest.getPathType() == PathType.DISCOVERY) {
            discoveryCache.remove(dest);
            return discoveryCache.getAddress();
        }
        return null;
    }
    /**
     * Notifyes ServiceDiscovery that a destination cant be reached
     * @param dest the faulty destination
     * @param serviceName optional name for the service that the address was for
     * @return a new destiantion if 
     */
    @Override
    public DestinationMetaData destinationUnreachable(DestinationMetaData dest, String serviceName) {
        AtomicInteger failCount = new AtomicInteger(1);
        failCount = this.destFailStat.putIfAbsent(dest, failCount);
        int value = 1;
        if(failCount != null ) {
             value = failCount.incrementAndGet();
        }
        
        // TODO: change to threashold over time
        if(value > 5) {
            return destinationRemove(dest,serviceName);
        }
        if (serviceName != null) {
            AddressPool pool = serviceCache.get(serviceName);
            if (pool != null) {
                return pool.getAddress();
            }
        }
        if (dest.getPathType() == PathType.DISCOVERY) {
            return discoveryCache.getAddress();
        }
        return null;
    }

    /**
     * Search globaly for a destination with a service named serviceName
     * @param destination in/out gets filled in with the address and pathtype
     * @param serviceName name of the service to search for.
     * @return
     */
    private RouteSignal searchForService(DestinationMetaData destination, String serviceName) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        // TODO: ask the other GD
        return signal;
    }

    /**
     * Finds the next hop destination, fills in destination, and return a RouteSignal
     * @param destination in/out gets filled in with the address and pathType
     * @param origin where this msg/packet came from
     * @param to a chain of services that should be visited next
     * @return where to reach the destinatino, local,net,nat,endpoint
     */
    @Override
    public RouteSignal getDestination(DestinationMetaData destination, Origin origin, ServiceChain to) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        String serviceName = null;
        if (to != null) {
            serviceName = to.peekNextServiceName();
        }
        if (serviceName == null) {
            signal = resolveOriginRoute(origin);
            destination.setSocketAddress(origin.getAddress());
            destination.setPathType(PathType.RELAY);// latch is a relay??
            return signal;
        }

        // TODO: check load factors and so on..
        if(this.localServiceTable != null && localServiceTable.getService(serviceName) != null) {
            signal = RouteSignal.LOCALDESTINATION;
            destination.setPathType(PathType.SERVICE);
            return signal;
        }
        // this is a GlobalServiceDiscovery so check cache first
        AddressPool pool = this.activeQueue.get(serviceName);
        if(pool == null){    
            pool = this.serviceCache.get(serviceName);
            if (pool == null) {
                return searchForService(destination, serviceName);
            }
        }

        DestinationMetaData addr = pool.getAddress();
        if (addr != null) {
            destination.setSocketAddress(addr.getSocketAddress());
            PathType path = addr.getPathType();
            path = (path == null) ? PathType.SERVICE : path; 
            destination.setPathType(path); // default pathType for now
            signal = RouteSignal.NETWORKDESTINATION;
        }

        return signal;
    }

    /**
     * This is used when a packet have arrived at the origin point, If this is a
     * keepalive bridge it gives back a networkdestination, else indicate if it
     * should go to a local subsystem or fill a serviceRequest
     *
     * @param origin must be this machine (localAddress) and not null
     * @return ENDPOINT,LOCALDESTINATION,NETWORKDESTINATION
     */
    private RouteSignal resolveLocalEndpoint(Origin origin) {
        if (origin.getSocketLatchID() != null) {
            return RouteSignal.BRIDGELATCH; // we are a nat bridge
        }
        if (origin.getServiceRequestID() == null) {
            return RouteSignal.LOCALDESTINATION;
        }
        return RouteSignal.ENDPOINT;
    }

    /**
     * This is used when a packet have arrived and its serviceChain is empty It
     * will then determine how to route the incoming data
     *
     * @param origin the origin part of the packet
     * @return RouteSignal
     */
    private RouteSignal resolveOriginRoute(Origin origin) {
        if (origin == null) {
            return RouteSignal.NOTFOUND;
        }

        InetSocketAddress address = (InetSocketAddress) origin.getAddress();

        if (address == null || address.equals((InetSocketAddress) localAddress)) {
            return resolveLocalEndpoint(origin);
        }
        return RouteSignal.RETURNTOORIGIN;
    }

    /**
     * This is used when a packet have arrived and gives back the correct route
     *
     * @param origin where the packet came from
     * @param to where the packet should go
     * @return where to route the data
     */
    @Override
    public RouteSignal getLocalInterface(Origin origin, ServiceChain to) {
        if (to == null || to.peekNextServiceName() == null) {
            return resolveOriginRoute(origin);
        }
        // TODO: add check for internal active services on this machine
        return RouteSignal.LOCALDESTINATION;
    }

    private void printAnnounceList(String[] nameList){
        System.out.println("Service list");
        for(String s: nameList){
            System.out.println("\t" + s);
        }
    }
    /**
     * Announces the available services on this node.
     *
     */
    @Override
    public boolean announce() {
        if(localServiceTable == null)
            return false;
//        InetSocketAddress destAddr = (InetSocketAddress) discoveryCache.getAddress();
//        if(destAddr == null)
//            return false;
        DestinationMetaData dest = discoveryCache.getAddress();
        if(dest == null)
            return false;
        KeySetView<String,ServiceMetaData> keys = localServiceTable.getKeySet();
        ArrayList<String> serviceList = new ArrayList<>();
        for(String key : keys){
            serviceList.add(key);
        }
        String[] nameList = serviceList.toArray(new String[serviceList.size()]);
        AnnouncePacket announce = new AnnouncePacket(localAddress,nameList);
        announce.setGlobalDiscovery(true);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.ANNOUNCE);
        packet.setAnnonce(announce);
        printAnnounceList(nameList);
        //DestinationMetaData dest = new DestinationMetaData(destAddr,PathType.DISCOVERY);
        boolean success = false;
        try{
            byte[] bytes = serializeToBytes(packet);
            success = internalRouting.SendToDestination(dest,bytes);
            if(!success){
                dest = destinationUnreachable(dest,null);
                success = internalRouting.SendToDestination(dest, bytes);
            }
        }catch(IOException ex){
            return false;
        }
        return true;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private byte[] serializeToBytes(Serializable data) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    @Override
    public void stop() {
        threadPool.shutdown();
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * updates the discovery lookup table
     *
     * @param origin the origin of the instance who sent update message
     * @param data a byte representation of a discoverypacket
     */
    @Override
    public void discoveryUpdate(Origin origin, byte[] data) {
        DiscoveryLookup lookup = new DiscoveryLookup(origin,data);
        threadPool.execute(lookup);
        
    }

    private DiscoveryPacket packetUnpack(byte[] data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }

    private void propagateTopology(TopologyPacket packet){
        int count = packet.decrementCount();
      	if(count <= 0)
            return;
      	DestinationMetaData nextHop = null;
        InetSocketAddress lastJump = (InetSocketAddress)packet.getLastJump();
        InetSocketAddress originalSource = (InetSocketAddress)packet.getOriginalSource();
        for(int i = 0; i < count; i++) {
            nextHop = this.discoveryCache.getAddress();
            if(nextHop.equals(lastJump) || nextHop.equals(originalSource))
                continue;
            DiscoveryPacket discPack = new DiscoveryPacket(DiscoveryPacketType.TOPOLOGY);
            packet.setLastJump(localAddress);
            discPack.setTopologyPacket(packet);
            boolean success = false;
            try{
                byte[] data = serializeToBytes(discPack);
                success = internalRouting.SendToDestination(nextHop,data);
                if(!success){
                    destinationUnreachable(nextHop,null);
                }
            }catch(IOException e){
                // TODO: LOGGING
            }
        }

    }

  	private void processTopologyPackets(TopologyPacket packet){
        
        this.discoveryCache.addAddress(packet.getInstanceAddress());
      	propagateTopology(packet);
  	}

    private void addService(DestinationMetaData addr,String name){
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.serviceCache.putIfAbsent(name,pool);
        if(oldPool != null){
            oldPool.addAddress(addr);
        }else{
            pool.addAddress(addr);
        }
    }

    // register announced services in global service discovery
    private void processAnnouncePacket(AnnouncePacket packet){
        String[] serviceList = packet.getServiceList();
        SocketAddress addr = packet.getInstanceAddress();
        if(addr == null){
            return;
        }
        
        DestinationMetaData sAddr = new DestinationMetaData(addr,PathType.SERVICE);
        ArrayList<QueuePacket> queueList = new ArrayList<>();
        for (String s : serviceList) {
            addService(sAddr, s);
            AddressPool addressPool = activeQueue.get(s);
            if(addressPool != null){
                DestinationMetaData dest = addressPool.getAddress();
                if(dest == null)
                    continue;
                QueuePacket queuePacket = new QueuePacket(dest.getSocketAddress(),serviceList);
                queueList.add(queuePacket);
                
            }
                                      
        }
        if(packet.isGlobalDiscovery()){
            DestinationMetaData globUpdate= new DestinationMetaData(addr,PathType.DISCOVERY);
            discoveryCache.addAddress(globUpdate);
          	TopologyPacket topology = new TopologyPacket(globUpdate,localAddress,4);
            propagateTopology(topology);
        }
        DestinationMetaData dest = new DestinationMetaData(addr,PathType.DISCOVERY);
        
        for(QueuePacket queuePacket: queueList){
            DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
            discoveryPacket.setQueue(queuePacket);
            try{
                byte[] data = serializeToBytes(discoveryPacket);
                if(!internalRouting.SendToDestination(dest,data)){
                    break;
                }
            }catch(IOException e){
                e.printStackTrace();
                // TODO: Logg error
            }
        }
        printAnnounceList(serviceList);
    }

    // it sends back a filled discovery request probe
    private void processProbeRequest(Origin origin, DiscoveryProbe probe){
        
        AddressPool pool = null;
        if((pool = this.activeQueue.get(probe.getName())) == null){
            pool = this.serviceCache.get(probe.getName());
        }
        
        if(pool == null){
            byte[] data = new byte[0];
            try{
            data = serializeToBytes(new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE));
            }catch(IOException e){}
            internalRouting.SendBackToOrigin(origin,PathType.DISCOVERY,data);
            return;
        }
        DestinationMetaData addr = pool.getAddress();
        probe.setAddress(addr);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE);
        packet.setProbe(probe);
        byte[] data = new byte[0];
        try{
            data = serializeToBytes(packet);
        }catch(IOException ex){
            
        }
        internalRouting.SendBackToOrigin(origin,PathType.DISCOVERY,data);
        
    }

    private void addQueue(DestinationMetaData addr,String queue){
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.activeQueue.putIfAbsent(queue,pool);
        if(oldPool != null){
            oldPool.addAddress(addr);
        }else{
            pool.addAddress(addr);
        }
    }

    private void processQueuePacket(QueuePacket packet){
        if(packet == null){
            return;
        }
        String[] queueList = packet.getRequestQueueList();
        SocketAddress addr = packet.getInstanceAddress();
        if(queueList == null){
            System.out.println("Queuelist is null in processQueuePacket GlobalServiceDiscovery");
            return;
        }
        DestinationMetaData qAddr = new DestinationMetaData(addr,PathType.REQUESTQUEUE);
        if (qAddr == null) {
            return;
        }
        System.out.println("Registering queues: " + qAddr.toString());
        for (String s : queueList) {
            System.out.println("\t" + s);
            addQueue(qAddr, s);
        }
    }

    /**
     * Finds the destination to the request queue.
     *
     * @param destination destination the destination address for the queueList
     * @param serviceName serviceName the name for the service that needs new work.
     */
    @Override
    public RouteSignal getRequestQueueDestination(DestinationMetaData destination, String serviceName){
        AddressPool pool = activeQueue.get(serviceName);
        if(pool == null)
            return RouteSignal.NOTFOUND;
        DestinationMetaData addr = pool.getAddress();
        if (addr == null || addr.getSocketAddress() == null){
            return RouteSignal.NOTFOUND;
        }
        InetSocketAddress socketAddress = (InetSocketAddress)addr.getSocketAddress();
        if(socketAddress.equals((InetSocketAddress) localAddress)) {
            destination.setPathType(addr.getPathType());
            return RouteSignal.LOCALDESTINATION;
        }
    
        destination.setSocketAddress(addr.getSocketAddress());
        destination.setPathType(addr.getPathType());
        return RouteSignal.NETWORKDESTINATION;
    }

    /**
     * Announces active queues 
     * @param queueList a list of names for the queues.
     */
    public boolean announceQueues(RequestQueueManager queueManager){
        String[] nameList = queueManager.getActiveQueues();
        DestinationMetaData dest = discoveryCache.getAddress();
        if(dest == null)
            return false;
        QueuePacket queuePacket = new QueuePacket(localAddress,nameList);
        DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
        discoveryPacket.setQueue(queuePacket);
        boolean success = false;
        try{
            byte[] data = serializeToBytes(discoveryPacket);
            success = internalRouting.SendToDestination(dest,data);
            if(!success){
                DestinationMetaData destination = destinationUnreachable(dest,null);
            	success = internalRouting.SendToDestination(destination,data);
            }
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        return success;
    }

    private DestinationMetaData[] connectedDiscoveryNodes() {
        return this.discoveryCache.copyPoolData();
    }
    
    private DestinationMetaData[] connectedServiceNode(String serviceName) {
        AddressPool pool = this.serviceCache.get(serviceName);
        if(pool == null) return new DestinationMetaData[0];
        return pool.copyPoolData();
    }
    
    private DestinationMetaData[] connectedRequestQueueNode(String serviceName) {
        AddressPool pool = this.activeQueue.get(serviceName);
        if(pool == null) return new DestinationMetaData[0];
        return pool.copyPoolData();
    }
    
    @Override
    public StatisticsData[] getStatisticsForSubSystem(String name) {
        if(name.equals("discoveryNodes")) {
            DestinationMetaData[] discoveryNodes = connectedDiscoveryNodes();
            if(discoveryNodes.length <= 0) return new StatisticsData[0];
            StatisticsData[] ret = new StatisticsData[1];
            ret[0] = new StatisticsData<DestinationMetaData>(StatType.SERVICEDISCOVERY,name,discoveryNodes);
            return ret;
        }
        
        if (name.equals("requestQueueNodes")) {
            ArrayList<StatisticsData> result = new ArrayList<>();
            for(Map.Entry<String,AddressPool>  entry: this.activeQueue.entrySet()) {
                DestinationMetaData[] nodes = connectedRequestQueueNode(entry.getKey());
                result.add(new StatisticsData(StatType.SERVICEDISCOVERY,entry.getKey(),nodes));
            }
            StatisticsData[] ret = result.toArray(new StatisticsData[result.size()]);
            return ret;
        }
        
        if (name.equals("serviceNodes")) {
            ArrayList<StatisticsData> result = new ArrayList<>();
            for(Map.Entry<String,AddressPool>  entry: this.serviceCache.entrySet()) {
                DestinationMetaData[] nodes = connectedServiceNode(entry.getKey());
                result.add(new StatisticsData(StatType.SERVICEDISCOVERY,entry.getKey(),nodes));
            }
            StatisticsData[] ret = result.toArray(new StatisticsData[result.size()]);
            return ret;
        }
        
        return new StatisticsData[0];
    }

    @Override
    public StatisticsData getStatistics(String[] name) {
        if(name[0].equals("discoveryNodes")) {
            DestinationMetaData[] discoveryNodes = connectedDiscoveryNodes();
            if(discoveryNodes.length <= 0) return new StatisticsData();
            return new StatisticsData(StatType.SERVICEDISCOVERY,name[0],discoveryNodes);
            
        }

        if(name.length <= 1)return new StatisticsData();
        if(name[0].equals("requestQueueNodes")) {
            DestinationMetaData[] reqQueueNodes = connectedRequestQueueNode(name[1]);
            if(reqQueueNodes.length <= 0) return new StatisticsData();
            return new StatisticsData(StatType.SERVICEDISCOVERY,name[1],reqQueueNodes);
            
        }
        
        if(name[0].equals("serviceNodes")) {
            DestinationMetaData[] servNodes = connectedServiceNode(name[1]);
            if(servNodes.length <= 0) return new StatisticsData();
            return new StatisticsData(StatType.SERVICEDISCOVERY,name[1],servNodes);
            
        }
        
        return new StatisticsData();
       
    }

    private class DiscoveryLookup implements Runnable {

        private Origin origin;
        private byte[] data;
        
        public DiscoveryLookup(Origin origin, byte[] data) {
            this.origin = origin;
            this.data = data;
        }

        @Override
        public void run() {
            DiscoveryPacket packet = packetUnpack(data);
            DiscoveryPacketType type = packet.getPacketType();
            //to do: switch not functioning properly with enums
//            System.out.println("DefaultGlobalServiceDiscovery: " + type
//                    + " from: " + ((InetSocketAddress) origin.getAddress()).toString());
            switch (type) {
            case DISCOVERYREQUEST:
                processProbeRequest(origin, packet.getProbe());
                break;
            case DISCOVERYRESPONSE:
                //localDiscovery.updateHandling(from, packet);
                break;
            case ANNOUNCE:
                processAnnouncePacket(packet.getAnnounce());
                break;
            case REQUESTQUEUE:
                processQueuePacket(packet.getQueue());
                break;
            default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                break;
            }
        }

    }
}
