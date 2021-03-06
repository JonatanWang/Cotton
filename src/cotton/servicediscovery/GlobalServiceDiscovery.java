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

import cotton.configuration.Configurator;
import cotton.internalrouting.InternalRoutingServiceDiscovery;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DefaultServiceChain;
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

import cotton.services.ServiceMetaData;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatType;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author magnus
 * @author Tony
 */
public class GlobalServiceDiscovery implements ServiceDiscovery {

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;
    private AddressPool discoveryCache;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private ConcurrentHashMap<String, AddressPool> sleepingServiceCache = new ConcurrentHashMap<String, AddressPool>();
    private ActiveServiceLookup localServiceTable = null;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, AddressPool> activeQueue;
    private ConcurrentHashMap<String, AddressPool> sleepingQueue = new ConcurrentHashMap<String, AddressPool>();
    private ConcurrentHashMap<DestinationMetaData, AtomicInteger> destFailStat = new ConcurrentHashMap();
    private RequestQueueManager queueManager;
    private final ScheduledThreadPoolExecutor deadAddressValidator;
    private ConcurrentLinkedQueue<ReapedAddress> deadAddresses;
    private LinkedBlockingQueue<UpdateItem> updateQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Command> poolCheckQueue = new LinkedBlockingQueue<>();

    private final boolean upScalingActive = true; // if we add queues to sleeping instead of active when they are announced
    private AtomicInteger runDownScaling = new AtomicInteger(1);
    private AtomicBoolean runCheckAddress = new AtomicBoolean(true);
    /**
     * Fills in a list of all pre set globalServiceDiscovery addresses
     *
     * @param globalDNS information from the config file
     */
    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        if (globalDNS != null) {
            SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
            for (int i = 0; i < addrArr.length; i++) {
                DestinationMetaData gAddr = new DestinationMetaData(addrArr[i], PathType.DISCOVERY);
                System.out.println("GlobalDiscovery adddress:" + gAddr.toString());
                discoveryCache.addAddress(gAddr);
            }
        }
    }
    
    private void initGlobalDiscoveryPool(Configurator conf) {
        if (conf != null) {
            SocketAddress[] addrArr = conf.getDiscoverySocketAddresses();
            for (int i = 0; i < addrArr.length; i++) {
                DestinationMetaData gAddr = new DestinationMetaData(addrArr[i], PathType.DISCOVERY);
                System.out.println("GlobalDiscovery adddress:" + gAddr.toString());
                discoveryCache.addAddress(gAddr);
            }
        }
    }

    public GlobalServiceDiscovery(GlobalDiscoveryDNS dnsConfig) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(dnsConfig);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        //threadPool = Executors.newCachedThreadPool();
        this.threadPool = Executors.newCachedThreadPool();//newFixedThreadPool(10);
        
        this.activeQueue = new ConcurrentHashMap<>();
        this.deadAddresses = new ConcurrentLinkedQueue<>();
        this.deadAddressValidator = new ScheduledThreadPoolExecutor(2);
        this.startDownScaling();
        this.startPoolcheckSystem(15000);
        this.startDeadAddressChecker(20000);
    }
    
    public GlobalServiceDiscovery(boolean config, Configurator conf) {
        for(SocketAddress s: conf.getDiscoverySocketAddresses())
            System.out.println("global sockets: " + s);
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(conf);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        threadPool = Executors.newCachedThreadPool();
        //this.threadPool = Executors.newFixedThreadPool(10);
        this.activeQueue = new ConcurrentHashMap<>();
        this.deadAddresses = new ConcurrentLinkedQueue<>();
        deadAddressValidator = new ScheduledThreadPoolExecutor(1);
        this.startDownScaling();
        this.startPoolcheckSystem(15000);
        this.startDeadAddressChecker(20000);
    }

    /**
     * So we can get out to other machines
     *
     * @param network GlobalServiceDiscovery way out to the world
     * @param localAddress what machine its one and its port
     */
    @Override
    public void setNetwork(InternalRoutingServiceDiscovery network, SocketAddress localAddress) {
        this.internalRouting = network;
        this.localAddress = localAddress;
        this.discoveryCache.addAddress(new DestinationMetaData(localAddress,PathType.DISCOVERY));
    }

    /**
     * If this machine runs local services set the lookup table
     *
     * @param serviceTable a table with all the local services that can be run
     * on this machine
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
     *
     * @param dest the faulty destination
     * @param serviceName optional name for the service that the address was for
     * @return a new destiantion if
     */
    @Override
    public DestinationMetaData destinationUnreachable(DestinationMetaData dest, String serviceName) {
        AtomicInteger failCount = null;
        AtomicInteger newFailCount = new AtomicInteger(1);
        if (dest == null) {
            System.out.println("global, destinationUnreachable: dest is null");
            return null;
        }
        failCount = this.destFailStat.putIfAbsent(dest, newFailCount);
        int value = 1;

        if (failCount != null) {
            value = failCount.incrementAndGet();
        } else {
            value = newFailCount.incrementAndGet();
        }
        PathType type = dest.getPathType();
        AddressPool currentPool = null;
        switch (type) {
            case REQUESTQUEUE:
                currentPool = activeQueue.get(serviceName);
                break;
            case DISCOVERY:
                currentPool = discoveryCache;
                break;
            case SERVICE:
                currentPool = serviceCache.get(serviceName);
                break;
        }
        // TODO: change to threashold over time
        if (value > 5) {
            DestinationMetaData newAddress = destinationRemove(dest, serviceName);
            this.deadAddresses.add(new ReapedAddress(serviceName, dest));
            if (currentPool.size() < 2) {
                //checkDeadAddresses();
            }
            return newAddress;
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
     *
     * @param destination in/out gets filled in with the address and pathtype
     * @param serviceName name of the service to search for.
     * @return
     */
    private RouteSignal searchForService(DestinationMetaData destination, String serviceName) {
        RouteSignal signal = RouteSignal.NOTFOUND;
        // TODO: ask the other GD
        return signal;
    }
    
    private void processCloudSnapshot(Origin origin) {
        String[] availableServices = this.getAvailableServices();        
        ArrayList<DiscoveryProbe> msg = new ArrayList<>();
        DestinationMetaData q = null;
        DestinationMetaData s = null;
        for (String name : availableServices) {
            AddressPool qget = this.activeQueue.get(name);
            if(qget != null && (q = qget.getAddress()) != null) {
                msg.add(new DiscoveryProbe(name,q));
            }
            AddressPool sget = this.activeQueue.get(name);
            if(sget != null && (s = sget.getAddress()) != null) {
                msg.add(new DiscoveryProbe(name,s));
            }            
        }
        CloudSnapshot cloudSnapshot = new CloudSnapshot(msg);
        try {
            byte[] data = serializeToBytes(cloudSnapshot);
            internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * This will try to ask another global discovery if they know about any
     * new services, if this discovery is missing one it will add it.
     * This is done so a global discovery at least know about one of each service
     */
    private void updateCloudAwareness() {
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.CLOUD_SNAPSHOT);
        DestinationMetaData tmp = discoveryCache.getAddress();
        if(tmp == null) {
            return;
        }
        if(tmp.compareAddress(this.localAddress)) {
            return;
        }
        DestinationMetaData dest = tmp.duplicate();
        byte[] data;
        try {
            data = serializeToBytes(packet);
        } catch (IOException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        ServiceRequest request = internalRouting.sendWithResponse(dest, data, 400);
        
        if(request == null || request.getData() == null) {
            return;
        }
        byte[] data1 = request.getData();
        CloudSnapshot cloudSnapshot = cloudSnapshotUnpack(data1);
        DiscoveryProbe[] snapshot = cloudSnapshot.getSnapshot();
        addSnapshot(snapshot);
        
    }

    private void addSnapshot(DiscoveryProbe[] snapshot) {
        if(snapshot == null)
            return;
        for (DiscoveryProbe s : snapshot) {
            DestinationMetaData dmd = s.getAddress();
            if (dmd.getPathType() == PathType.SERVICE) {
                this.addService(dmd, s.getName());
            } else if (dmd.getPathType() == PathType.REQUESTQUEUE) {
                this.addQueue(dmd, s.getName());
            }
        }
    }
    
    /**
     * Finds the next hop destination, fills in destination, and return a
     * RouteSignal
     *
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
        if (this.localServiceTable != null && localServiceTable.getService(serviceName) != null) {
            signal = RouteSignal.LOCALDESTINATION;
            destination.setPathType(PathType.SERVICE);
            return signal;
        }
        // this is a GlobalServiceDiscovery so check cache first
        AddressPool pool = this.activeQueue.get(serviceName);
        if (pool == null) {
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
            return signal;
        }
        System.out.println("Last return (global: getDestination): " + serviceName + " Signal: " + signal);
        return signal;
    }
    
    /**
     * Returns one DestinationMetaData for a type 
     * @param type what type: DISCOVERY,SERVICE,REQUESTQUEUE
     * @param name if null for discovery, else need to specify
     * @return one address or null if not found
     */
    @Override
    public DestinationMetaData getDestinationForType(PathType type,String name) {
        DestinationMetaData ret = null;
        AddressPool pool = null;
        switch(type) {
            case DISCOVERY:
                pool = this.discoveryCache;
                break;
            case SERVICE:
                if(name == null) {
                   return null;
                }
                pool = this.serviceCache.get(name);
                break;
            case REQUESTQUEUE:
                if(name == null) {
                   return null;
                }
                pool = this.activeQueue.get(name);
                break;
        }
        if(pool == null) {
            return null;
        }
        return pool.getAddress();
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
            System.out.println("resolveOriginRoute: Origin null");
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

    private void printAnnounceList(String[] nameList) {
        System.out.println("Service list");
        for (String s : nameList) {
            System.out.println("\t" + s);
        }
    }

    /**
     * Announces the available services on this node.
     *
     */
    @Override
    public boolean announce() {
        if (localServiceTable == null) {
            return false;
        }
//        InetSocketAddress destAddr = (InetSocketAddress) discoveryCache.getAddress();
//        if(destAddr == null)
//            return false;
        DestinationMetaData dest = discoveryCache.getAddress();
        if (dest == null) {
            return false;
        }
        ArrayList<ConfigEntry> entryList = new ArrayList<>();
        Set<Map.Entry<String, ServiceMetaData>> keys = localServiceTable.getEntrySet();
        int totalServiceCapacity = 0;
        for (Map.Entry<String, ServiceMetaData> entry : keys) {
            ServiceMetaData metaData = entry.getValue();
            int maxCapacity = metaData.getMaxCapacity();
            totalServiceCapacity += maxCapacity;
            entryList.add(new ConfigEntry(entry.getKey(), maxCapacity, PathType.SERVICE, ServiceStatus.ACTIVE));
        }

        int totalQueueCapacity = 0;
        int maxAmountOfQueues = 0;
        if (queueManager != null) {
            String[] nameList = this.queueManager.getActiveQueues();
            // String[] nameList = serviceList.toArray(new String[serviceList.size()]);
            for (String s : nameList) {
                int maxCapacity = queueManager.getMaxCapacity(s);
                totalQueueCapacity += maxCapacity;
                entryList.add(new ConfigEntry(s, maxCapacity, PathType.REQUESTQUEUE, ServiceStatus.ACTIVE));
                addQueue(new DestinationMetaData(localAddress, PathType.REQUESTQUEUE), s);
            }
            maxAmountOfQueues = queueManager.getMaxAmountOfQueues();
        }
        ConfigurationPacket configPacket = new ConfigurationPacket(localAddress, maxAmountOfQueues, totalServiceCapacity, true);
        configPacket.setConfigEntry(entryList.toArray(new ConfigEntry[entryList.size()]));
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.CONFIG);
        packet.setConfigPacket(configPacket);

        //DestinationMetaData dest = new DestinationMetaData(destAddr,PathType.DISCOVERY);
        boolean success = false;
        try {
            byte[] bytes = serializeToBytes(packet);
            success = internalRouting.sendToDestination(dest, bytes);
            if (!success) {
                dest = destinationUnreachable(dest, null);
                success = internalRouting.sendToDestination(dest, bytes);
            }
        } catch (IOException ex) {
            return false;
        }
        if(success){
            this.updateCloudAwareness();
        }
        return true;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }
    
    private void validateServiceActivity() {
        AddressPool pool;
        DestinationMetaData[] destinations;
        for (Map.Entry<String, AddressPool> entry : serviceCache.entrySet()) {
            pool = entry.getValue();
            String key = entry.getKey();
            checkPoolReachabillity(pool, key);
        }
    }
    
    private void validateQueueActivity() {
        AddressPool pool;
        DestinationMetaData[] destinations;
        for (Map.Entry<String, AddressPool> entry : this.activeQueue.entrySet()) {
            pool = entry.getValue();
            String key = entry.getKey();
            checkPoolReachabillity(pool, key);
        }
    }
    
    private void startPoolcheckSystem(int checkInterval) {
        final Runnable spcs = new Runnable() {
            @Override
            public void run() {
                while (runCheckAddress.get()) {
                    try {
                        Thread.sleep(checkInterval);
                    } catch (InterruptedException ex) {}
                    validateServiceActivity();
                    validateQueueActivity();
                }
            }
        };
        
        final Runnable spcs2 = new Runnable() {
            @Override
            public void run() {
                while (runCheckAddress.get()) {
                    Command cmd = null;
                    try {
                        cmd = poolCheckQueue.take();
                    } catch (InterruptedException ex) {}
                    if( cmd == null || cmd.getCommandType() != CommandType.CHECK_REACHABILLITY) {
                        continue;             
                    }
                    String name = null;
                    if((name = cmd.getName()) == null) {
                        validateServiceActivity();
                    }else {
                        AddressPool pool = serviceCache.get(name);
                        checkPoolReachabillity(pool, name);
                    }
                }
            }
        };
        this.threadPool.execute(spcs);
        this.threadPool.execute(spcs2);
    }
    private void checkPoolReachabillity(AddressPool pool, String key) {
        if (pool == null || key == null) {
            return;
        }
        DestinationMetaData[] destinations = pool.copyPoolData();
        for (int i = 0; i < destinations.length; i++) {
            DiscoveryProbe probe = new DiscoveryProbe(key, destinations[i]);
            DiscoveryPacket response = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
            response.setProbe(probe);
            byte[] data = null;
            try {
                data = serializeToBytes(response);
            } catch (IOException e) {
            }
            DestinationMetaData target = new DestinationMetaData(destinations[i].getSocketAddress(), PathType.DISCOVERY);
            ServiceRequest request = internalRouting.sendWithResponse(target, data, 70);
            if (request == null || request.getData() == null) {
                boolean flag = pool.remove(destinations[i]);
                DestinationMetaData d = new DestinationMetaData(destinations[i]);
                this.deadAddresses.add(new ReapedAddress(key,d));
            }
        }
    }

    private boolean sendHearthBeat(String name, DestinationMetaData destination) {
        DestinationMetaData dest= new DestinationMetaData(destination);
        DiscoveryProbe probe = new DiscoveryProbe(name, dest);
        DiscoveryPacket response = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
        response.setProbe(probe);
        byte[] data = null;
        try {
            data = serializeToBytes(response);
        } catch (IOException e) {
        }
        DestinationMetaData target = new DestinationMetaData(dest.getSocketAddress(), PathType.DISCOVERY);
        ServiceRequest request = this.internalRouting.sendWithResponse(target, data, 70);
        if (request != null && request.getData() != null) {
            return true;
        }
        return true;
    }
    
    private AddressPool getPool(PathType pt,String name) {
        AddressPool pool = null;
        switch(pt){
            case DISCOVERY:
                pool = this.discoveryCache;
                break;
            case REQUESTQUEUE:
                pool = this.activeQueue.get(name);
                break;
            case SERVICE:
                pool = this.serviceCache.get(name);
                break;
            default:
                break;
        }
        return pool;
    }
    
    private void startDeadAddressChecker(int checkInterval) {
        this.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (runCheckAddress.get()) {
                    checkDeadAddresses();
                    try {
                        Thread.sleep(checkInterval);
                    } catch (InterruptedException ex) {}
                }
            }
        });
     
    }
    
    private void checkDeadAddresses() {
        ConcurrentLinkedQueue<ReapedAddress> reapedAddresses = this.deadAddresses;

        InternalRoutingServiceDiscovery routing = this.internalRouting;

        //System.out.println("GlobalServiceDiscovery:checkDeadAddresses:runnable");
        ReapedAddress addr = null;
        AddressPool pool = null;
        //ArrayList<ReapedAddress> left = new ArrayList<>();
        while ((addr = reapedAddresses.poll()) != null) {
            DiscoveryProbe probe = new DiscoveryProbe(addr.getName(), addr.getReapedAddress());
            DiscoveryPacket response = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
            response.setProbe(probe);
            byte[] data = null;
            try {
                data = serializeToBytes(response);
            } catch (IOException e) {
            }
            DestinationMetaData target = new DestinationMetaData(addr.getReapedAddress().getSocketAddress(), PathType.DISCOVERY);
            ServiceRequest request = routing.sendWithResponse(target, data, 70);
            if (request != null && request.getData() != null) {
                pool = getPool(addr.getReapedAddress().getPathType(), addr.getName());
                if (pool != null) {
                    pool.addAddress(addr.getReapedAddress());
                }
            } else {
                //left.add(addr);
                System.out.println("Address droped: " + addr);
            }
        }

    }

    

    @Override
    public void stop() {
        runCheckAddress.set(false);
        threadPool.shutdownNow();
        deadAddressValidator.shutdownNow();
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private AtomicInteger threadCount = new AtomicInteger(0);

    /**
     * updates the discovery lookup table
     *
     * @param origin the origin of the instance who sent update message
     * @param data a byte representation of a discoverypacket
     */
    @Override
    public void discoveryUpdate(Origin origin, byte[] data) {
        if(runCheckAddress.get() == false) {
            return;
        }
        if (this.threadCount.get() < 5 || (this.updateQueue.size() > 10 && this.threadCount.get() < 20 )) {
            this.threadCount.incrementAndGet();
            DiscoveryLookup lookup = new DiscoveryLookup(origin, data);
            threadPool.execute(lookup);
            return;
        }
        this.updateQueue.offer(new UpdateItem(origin, data));
    }

    private DiscoveryPacket packetUnpack(byte[] data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }
    
    private CloudSnapshot cloudSnapshotUnpack(byte[] data) {
        CloudSnapshot snap = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            snap = (CloudSnapshot) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return snap;
    }
    

    private void propagateTopology(TopologyPacket packet) {
        int count = packet.decrementCount();
        if (count <= 0) {
            return;
        }
        DestinationMetaData nextHop = null;
        InetSocketAddress lastJump = (InetSocketAddress) packet.getLastJump();
        InetSocketAddress originalSource = (InetSocketAddress) packet.getOriginalSource();
        for (int i = 0; i < count; i++) {
            nextHop = this.discoveryCache.getAddress();
            if (nextHop.equals(lastJump) || nextHop.equals(originalSource)) {
                continue;
            }
            DiscoveryPacket discPack = new DiscoveryPacket(DiscoveryPacketType.TOPOLOGY);
            packet.setLastJump(localAddress);
            discPack.setTopologyPacket(packet);
            boolean success = false;
            try {
                byte[] data = serializeToBytes(discPack);
                success = internalRouting.sendToDestination(nextHop, data);
                if (!success) {
                    destinationUnreachable(nextHop, null);
                }
            } catch (IOException e) {
                // TODO: LOGGING
            }
        }

    }

    private void processTopologyPackets(TopologyPacket packet) {

        this.discoveryCache.addAddress(packet.getInstanceAddress());
        propagateTopology(packet);
    }

    private void addService(DestinationMetaData addr, String name) {
        if(addr == null || name == null)
            return;
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.serviceCache.putIfAbsent(name, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
    }

    // register announced services in global service discovery
    private void processAnnouncePacket(AnnouncePacket packet) {
        String[] serviceList = packet.getServiceList();
        SocketAddress addr = packet.getInstanceAddress();
        if (addr == null) {
            return;
        }

        DestinationMetaData sAddr = new DestinationMetaData(addr, PathType.SERVICE);
        ArrayList<QueuePacket> queueList = new ArrayList<>();
        for (String s : serviceList) {
            addService(sAddr, s);
            AddressPool addressPool = activeQueue.get(s);
            if (addressPool != null) {
                DestinationMetaData dest = addressPool.getAddress();
                if (dest == null) {
                    continue;
                }
                QueuePacket queuePacket = new QueuePacket(dest.getSocketAddress(), serviceList);
                queueList.add(queuePacket);

            }

        }
        if (packet.isGlobalDiscovery()) {
            DestinationMetaData globUpdate = new DestinationMetaData(addr, PathType.DISCOVERY);
            discoveryCache.addAddress(globUpdate);
            TopologyPacket topology = new TopologyPacket(globUpdate, localAddress, 4);
            propagateTopology(topology);
        }
        DestinationMetaData dest = new DestinationMetaData(addr, PathType.DISCOVERY);

        for (QueuePacket queuePacket : queueList) {
            DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
            discoveryPacket.setQueue(queuePacket);
            try {
                byte[] data = serializeToBytes(discoveryPacket);
                if (!internalRouting.sendToDestination(dest, data)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Logg error
            }
        }
        printAnnounceList(serviceList);
    }

    // it sends back a filled discovery request probe
    private void processProbeRequest(Origin origin, DiscoveryProbe probe) {

        AddressPool pool = null;
        if (probe.getName() != null && (pool = this.activeQueue.get(probe.getName())) == null) {
            pool = this.serviceCache.get(probe.getName());
        }
        if (probe.getAddress() != null) {
            if (origin.getAddress().equals(probe.getAddress().getSocketAddress()) && probe.getName() != null) {
                byte[] tmp = new byte[0];
                try {
                    tmp = serializeToBytes(new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, tmp);
            }
        }

        if (pool == null) {
            byte[] data = new byte[0];
            try {
                data = serializeToBytes(new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE));
            } catch (IOException e) {
            }
            internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, data);
            return;
        }
        DestinationMetaData addr = pool.getAddress();
        probe.setAddress(addr);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE);
        packet.setProbe(probe);
        byte[] data = new byte[0];
        try {
            data = serializeToBytes(packet);
        } catch (IOException ex) {

        }
        internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, data);

    }

    private void sendToPool(byte[] data, AddressPool pool, PathType type) {
        if (pool == null) {
            return;
        }
        int size = pool.size();
        DestinationMetaData dest = null;
        for (int i = 0; i < size; i++) {
            DestinationMetaData tmp = pool.getAddress();
            if (type != null) {
                dest = new DestinationMetaData(tmp);
                dest.setPathType(type);
            } else {
                dest = tmp;
            }
            this.internalRouting.sendToDestination(dest, data);
        }
    }

    private void addQueue(DestinationMetaData addr, String queue) {
        if(addr == null || queue == null)
            return;
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.activeQueue.putIfAbsent(queue, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
        QueuePacket q = new QueuePacket(addr.getSocketAddress(), queue);
        DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
        discoveryPacket.setQueue(q);
        byte[] data;
        try {
            data = serializeToBytes(discoveryPacket);
        } catch (IOException e) {
            return;
        }
        sendToPool(data, this.serviceCache.get(queue), PathType.DISCOVERY);
    }
    
    private void notifyRemoveQueue(String queue,DestinationMetaData addr) {
        QueuePacket q = new QueuePacket(addr.getSocketAddress(), queue);
        q.setShouldRemove(true);;
        DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
        discoveryPacket.setQueue(q);
        byte[] data;
        try {
            data = serializeToBytes(discoveryPacket);
        } catch (IOException e) {
            return;
        }
        sendToPool(data, this.serviceCache.get(queue), PathType.DISCOVERY);
    }

    private void removeQueueFromPool(QueuePacket packet) {
        String[] qname = packet.getRequestQueueList();
        if(qname == null || qname.length < 1) {
            return;
        }
        AddressPool pool = this.activeQueue.get(qname[0]);
        if(pool == null || pool.size() < 1) {
            return;
        }
        DestinationMetaData dest = new DestinationMetaData(packet.getInstanceAddress(),PathType.REQUESTQUEUE);
        boolean succsess = pool.remove(dest);
        if(!succsess) {
            System.out.println("LocalServiceDiscovery:removeQueueFromPool: failed to remove queue");
            return;
        }else {
            this.addToSleepingQueue(dest, qname[0]);
        }
    }
    
    private void processQueuePacket(QueuePacket packet) {
        if (packet == null) {
            return;
        }
        if(packet.isShouldRemove()) {
            removeQueueFromPool(packet);
        }
        String[] queueList = packet.getRequestQueueList();
        SocketAddress addr = packet.getInstanceAddress();
        if (queueList == null) {
            System.out.println("Queuelist is null in processQueuePacket GlobalServiceDiscovery");
            return;
        }
        DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.REQUESTQUEUE);
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
     * @param serviceName serviceName the name for the service that needs new
     * work.
     */
    @Override
    public RouteSignal getRequestQueueDestination(DestinationMetaData destination, String serviceName) {
        AddressPool pool = activeQueue.get(serviceName);
        if (pool == null) {
            return RouteSignal.NOTFOUND;
        }
        DestinationMetaData addr = pool.getAddress();
        if (addr == null || addr.getSocketAddress() == null) {
            return RouteSignal.NOTFOUND;
        }
        InetSocketAddress socketAddress = (InetSocketAddress) addr.getSocketAddress();
        if (socketAddress.equals((InetSocketAddress) localAddress)) {
            destination.setPathType(addr.getPathType());
            return RouteSignal.LOCALDESTINATION;
        }

        destination.setSocketAddress(addr.getSocketAddress());
        destination.setPathType(addr.getPathType());
        return RouteSignal.NETWORKDESTINATION;
    }

    /**
     * Announces active queues.
     */
    public boolean announceQueues(RequestQueueManager queueManager) {
        this.queueManager = queueManager;
        return true;
    }

    private void triggeredCircuitBreaker(Origin origin, CircuitBreakerPacket circuit) {
        AddressPool pool;

        System.out.println("INCOMMING CIRCUITBREAKER MESSAGE IN GLOBAL SERVICE DISCOVERY: " + circuit.getCircuitName());
        pool = serviceCache.get(circuit.getCircuitName());
        if (pool == null) {
            System.out.println("GlobalServiceDiscovery:triggeredCircuitBreaker:No service pool");
            notifyCircuitBreakerLogger(circuit);
            return;
        }
        DestinationMetaData dmd = pool.getAddress();
        if(dmd == null) {
            System.out.println("GlobalServiceDiscovery:triggeredCircuitBreaker:No service registered");
            notifyCircuitBreakerLogger(circuit);
            return;
        }
        DestinationMetaData dest = new DestinationMetaData(dmd);
        dest.setPathType(PathType.COMMANDCONTROL);
        Command com = new Command(StatType.SERVICEHANDLER, circuit.getCircuitName(), null, 20, CommandType.CHANGE_ACTIVEAMOUNT);
        sendCommandPacket(dest, com);
        AddressPool sqPool = this.sleepingQueue.get(circuit.getCircuitName());
        if(sqPool != null) {
            DestinationMetaData qaddress = sqPool.getAddress();
            if(qaddress != null) {
                sqPool.remove(qaddress);
                this.addQueue(qaddress, circuit.getCircuitName());
            }
        }
        AddressPool ssPool = this.sleepingServiceCache.get(circuit.getCircuitName());
        if(ssPool != null) {
            DestinationMetaData saddress = ssPool.getAddress();
            if(saddress != null) {
                ssPool.remove(saddress);
                this.addService(saddress, circuit.getCircuitName());
            }
        }
        notifyCircuitBreakerLogger(circuit);
    }

    private void notifyCircuitBreakerLogger(CircuitBreakerPacket circuit) {
        AddressPool logg = this.serviceCache.get("CircuitBreakerLogger");
        DestinationMetaData loggdest;
        if(logg != null && (loggdest = logg.getAddress()) != null) {
            try {
                DestinationMetaData duplicate = loggdest.duplicate();
                byte[] msg = this.serializeToBytes(circuit);
                this.internalRouting.sendToDestination(duplicate,new DefaultServiceChain("CircuitBreakerLogger"), msg);
            } catch (IOException ex) {
                Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private String[] getAvailableServices() {
        ArrayList<String> str = new ArrayList<>();
        for (Map.Entry<String, AddressPool> entry : this.serviceCache.entrySet()) {
            str.add(entry.getKey());
        }
        return str.toArray(new String[str.size()]);
    }
    
    @Override
    public StatisticsData[] processCommand(Command command) {
        StatisticsData[] retSystem;
        CommandType cmdType = command.getCommandType();
        if (cmdType == CommandType.CHECK_REACHABILLITY) {
//            String name = command.getName();
//            threadPool.execute(new Runnable() {
//                @Override
//                public void run() {
//                    if(name != null) {
//                        
//                    }
//                    validateServiceActivity();
//                }
//            });
            this.poolCheckQueue.offer(command);
            return new StatisticsData[0];
        } else if(cmdType == CommandType.ACTIVE_SERVICES) {
            StatisticsData<String>[] ret = new StatisticsData[1];
            ret[0] = new StatisticsData<String>();
            String[] ser = getAvailableServices();
            ret[0].setData(ser);
            return ret;
        }else if (cmdType == CommandType.STATISTICS_FORSUBSYSTEM){
            return retSystem = this.getStatisticsForSubSystem(command.getName());
        } else if (cmdType == CommandType.STATISTICS_FORSYSTEM) {
            StatisticsData[] retForSystem = new StatisticsData[1];
            retForSystem[0] = this.getStatistics(command.getTokens());
            return retForSystem;
        }
        return null;
    }

    private boolean sendCommandPacket(DestinationMetaData destination, Command command) {
        byte[] data;
        try {
            data = serializeToBytes(command);
        } catch (IOException e) {
            return false;
        }
        //destination.setPathType(PathType.COMMANDCONTROL);
        this.internalRouting.sendToDestination(destination, data);
        return true;
    }

    private boolean processLocalCommand(Command command) {
        byte[] data;
        try {
            data = serializeToBytes(command);
        } catch (IOException e) {
            return false;
        }
        DestinationMetaData destination = new DestinationMetaData(localAddress, PathType.COMMANDCONTROL);
        this.internalRouting.sendLocal(destination, RouteSignal.LOCALDESTINATION, data);
        return true;
    }

    private final AtomicInteger queuePoolScaling = new AtomicInteger(0);
    private void addToSleepingQueue(DestinationMetaData qAddr,String name) {
        if(qAddr == null) {
            return;
        }
        AddressPool pool = this.sleepingQueue.get(name);
        if(pool == null) {
            pool = new AddressPool();
            AddressPool tmp = this.sleepingQueue.putIfAbsent(name, pool);
            pool = (tmp == null) ? pool : tmp;
        }
        pool.addAddress(qAddr);
        
    }
    
    private class DownScalingItem {
        public String name;
        public DestinationMetaData dest;

        public DownScalingItem(String name, DestinationMetaData dest) {
            this.name = name;
            this.dest = dest;
        }

        public String getName() {
            return name;
        }

        public DestinationMetaData getDest() {
            return dest;
        }
        
    }
    private void startDownScaling() {
        final Runnable squeue = new Runnable(){
            @Override
            public void run() {
                while(runDownScaling.get() == 2) {
                    Set<Map.Entry<String, AddressPool>> entrySet = activeQueue.entrySet();
                    ArrayList<DownScalingItem> removed = new ArrayList<>();
                    for (Map.Entry<String, AddressPool> entry : entrySet) {
                        AddressPool p = entry.getValue();
                        if(p == null || p.size() < 3) {
                            continue;
                        }
                        DestinationMetaData address = p.getAddress();
                        if(address == null) {
                            continue;
                        }
                        removed.add(new DownScalingItem(entry.getKey() , address));
                        addToSleepingQueue(address,entry.getKey());
                        
                        notifyRemoveQueue(entry.getKey(),address);
                        
                    }
                    for (DownScalingItem item : removed) {
                        AddressPool get = activeQueue.get(item.getName());
                        get.remove(item.getDest());
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                    }
                    
                }
            }
        };
        if(this.runDownScaling.getAndSet(2) == 1) {
            this.threadPool.execute(squeue);
        }
    }
    
    private void processConfigPacket(ConfigurationPacket packet) {
        System.out.println("processConfigPacket: ");
        if (packet == null) {
            System.out.println("GlobalServiceDiscovery:processConfigPacket: packet null");
            return;
        }
        
        SocketAddress addr = packet.getInstanceAddress();
        ConfigEntry[] entry = packet.getConfigEntry();
        ArrayList<QueuePacket> availableQueues = new ArrayList<>();
        for (int i = 0; i < entry.length; i++) {
            ConfigEntry configEntry = entry[i];
            System.out.println("configEntry: " + i + " " + configEntry.getName() + " pathtype" + configEntry.getPathType());
            if (configEntry.getPathType() == PathType.REQUESTQUEUE) {
                int qcount = queuePoolScaling.incrementAndGet();
                DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.REQUESTQUEUE);
                AddressPool p = this.activeQueue.get(configEntry.getName());
                if (upScalingActive && (p != null && p.size() < 1)) {
                    addToSleepingQueue(qAddr, configEntry.getName());
                } else {
                    addQueue(qAddr, configEntry.getName());
                }
            } else if (configEntry.getPathType() == PathType.SERVICE) {
                DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.SERVICE);
                addService(qAddr, configEntry.getName());
                AddressPool pool = this.activeQueue.get(configEntry.getName());
                if (pool == null) {
                    continue;
                }
                DestinationMetaData metaData = pool.getAddress();
                if (metaData == null) {
                    continue;
                }
                availableQueues.add(new QueuePacket(metaData.getSocketAddress(), configEntry.getName()));
            }
        }
        boolean isGlobal = packet.isGlobalServiceDiscovery();
        if (isGlobal) {            
            discoveryCache.addAddress(new DestinationMetaData(addr, PathType.DISCOVERY));
        }

        DestinationMetaData returnAddress = new DestinationMetaData(addr, PathType.DISCOVERY);
        for (QueuePacket queues : availableQueues) {
            DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
            discoveryPacket.setQueue(queues);
            try {
                byte[] data = serializeToBytes(discoveryPacket);
                if (!internalRouting.sendToDestination(returnAddress, data)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Logg error
            }
        }
        if(this.discoveryCache.size() > 0 && !isGlobal) {
            DiscoveryPacket discoveryPacket = new DiscoveryPacket(DiscoveryPacketType.CONFIG);
            DestinationMetaData gd = this.discoveryCache.getAddress();
            
            if(gd == null ) {
                return;
            }
            SocketAddress sa = (InetSocketAddress) gd.getSocketAddress();
            if(this.localAddress.equals(sa)) {
                sa = gd.getSocketAddress();
            }
            
            ConfigurationPacket confp = new ConfigurationPacket(sa,0,0,true);
            discoveryPacket.setConfigPacket(confp);
            try {
                byte[] data2 = serializeToBytes(discoveryPacket);
                boolean sendToDestination = internalRouting.sendToDestination(returnAddress.duplicate(), data2);                        
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Logg error
            }
        }
        
    }

    private DestinationMetaData[] connectedDiscoveryNodes() {
        return this.discoveryCache.copyPoolData();
    }

    private DestinationMetaData[] connectedServiceNode(String serviceName) {
        AddressPool pool = this.serviceCache.get(serviceName);
        if (pool == null) {
            return new DestinationMetaData[0];
        }
        return pool.copyPoolData();
    }

    private DestinationMetaData[] connectedRequestQueueNode(String serviceName) {
        AddressPool pool = this.activeQueue.get(serviceName);
        if (pool == null) {
            return new DestinationMetaData[0];
        }
        return pool.copyPoolData();
    }

    @Override
    public StatisticsData[] getStatisticsForSubSystem(String name) {
        if (name.equals("discoveryNodes")) {
            DestinationMetaData[] discoveryNodes = connectedDiscoveryNodes();
            if (discoveryNodes.length <= 0) {
                return new StatisticsData[0];
            }
            StatisticsData[] ret = new StatisticsData[1];
            ret[0] = new StatisticsData<DestinationMetaData>(StatType.DISCOVERY, name, discoveryNodes);
            return ret;
        }

        if (name.equals("requestQueueNodes")) {
            ArrayList<StatisticsData> result = new ArrayList<>();
            for (Map.Entry<String, AddressPool> entry : this.activeQueue.entrySet()) {
                DestinationMetaData[] nodes = connectedRequestQueueNode(entry.getKey());
                result.add(new StatisticsData(StatType.DISCOVERY, entry.getKey(), nodes));
            }
            StatisticsData[] ret = result.toArray(new StatisticsData[result.size()]);
            return ret;
        }

        if (name.equals("serviceNodes")) {
            ArrayList<StatisticsData> result = new ArrayList<>();
            for (Map.Entry<String, AddressPool> entry : this.serviceCache.entrySet()) {
                DestinationMetaData[] nodes = connectedServiceNode(entry.getKey());
                result.add(new StatisticsData(StatType.DISCOVERY, entry.getKey(), nodes));
            }
            StatisticsData[] ret = result.toArray(new StatisticsData[result.size()]);
            return ret;
        }

        return new StatisticsData[0];
    }

    @Override
    public StatisticsData getStatistics(String[] name) {
        if (name[0].equals("discoveryNodes")) {
            DestinationMetaData[] discoveryNodes = connectedDiscoveryNodes();
            if (discoveryNodes.length <= 0) {
                return new StatisticsData();
            }
            return new StatisticsData(StatType.DISCOVERY, name[0], discoveryNodes);

        }

        if (name.length <= 1) {
            return new StatisticsData();
        }
        if (name[0].equals("requestQueueNodes")) {
            DestinationMetaData[] reqQueueNodes = connectedRequestQueueNode(name[1]);
            if (reqQueueNodes.length <= 0) {
                return new StatisticsData();
            }
            return new StatisticsData(StatType.DISCOVERY, name[1], reqQueueNodes);

        }

        if (name[0].equals("serviceNodes")) {
            DestinationMetaData[] servNodes = connectedServiceNode(name[1]);
            if (servNodes.length <= 0) {
                return new StatisticsData();
            }
            return new StatisticsData(StatType.DISCOVERY, name[1], servNodes);

        }

        return new StatisticsData();

    }

    @Override
    public StatisticsProvider getProvider() {
        return this;
    }

    @Override
    public StatType getStatType() {
        return StatType.DISCOVERY;
    }

    @Override
    public void requestQueueMessage(DiscoveryPacket packet) {
        Origin origin = new Origin();
        origin.setAddress(localAddress);
        decodeDiscoveryPacket(origin, packet);
    }

    private void removeNode(SocketAddress addr) {
        DestinationMetaData rq = new DestinationMetaData(addr,PathType.REQUESTQUEUE);
        DestinationMetaData sh = new DestinationMetaData(addr,PathType.SERVICE);
        AddressPool pool;
        for (Map.Entry<String, AddressPool> entry : this.activeQueue.entrySet()) {
            pool = entry.getValue();
            while(pool.remove(rq)){}
        }
        for (Map.Entry<String, AddressPool> entry : this.serviceCache.entrySet()) {
            pool = entry.getValue();
            while(pool.remove(sh)){}
        }
    }
    
    private void decodeDiscoveryPacket(Origin origin, DiscoveryPacket packet) {
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
            case TOPOLOGY:
                processTopologyPackets(packet.getTopology());
                break;
            case CLOUD_SNAPSHOT:
                processCloudSnapshot(origin);
                break;
            case ANNOUNCE:
                processAnnouncePacket(packet.getAnnounce());
                break;
            case REQUESTQUEUE:
                processQueuePacket(packet.getQueue());
                break;
            case CONFIG:
                System.out.println("Incomming config packet");
                processConfigPacket(packet.getConfigPacket());
                break;
            case NODE_SHUTDOWN:
                removeNode(packet.getAddress());
                break;
            case CIRCUITBREAKER:
                triggeredCircuitBreaker(origin, packet.getCircuitBreakerPacket());
                break;
            default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                break;
        }
    }

    private class UpdateItem {

        private Origin origin;
        private byte[] data;

        public UpdateItem(Origin origin, byte[] data) {
            this.origin = origin;
            this.data = data;
        }

        public Origin getOrigin() {
            return origin;
        }

        public byte[] getData() {
            return data;
        }

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
            decodeDiscoveryPacket(origin, packet);
            int loop = 5;
            while (loop > 0) {
                UpdateItem take = null;
                try {
                    take = updateQueue.take();
                    packet = packetUnpack(take.getData());
                    decodeDiscoveryPacket(take.getOrigin(), packet);

//                    take = updateQueue.poll();
//                    if (take == null) {
//                        loop--;
//                        continue;
//                    }
//                    packet = packetUnpack(take.getData());
//                    decodeDiscoveryPacket(take.getOrigin(), packet);
                } catch (InterruptedException ex) {}

                loop = 0;

            }
            threadCount.decrementAndGet();
        }
    }

    private class ReapedAddress {

        private String name;
        private DestinationMetaData reapedAddress;

        public ReapedAddress(String name, DestinationMetaData reapedAddress) {
            this.name = name;
            this.reapedAddress = reapedAddress.duplicate();
        }

        public String getName() {
            return this.name;
        }

        public DestinationMetaData getReapedAddress() {
            return this.reapedAddress;
        }

        @Override
        public String toString() {
            return "ReapedAddress{" + "name=" + name + ", reapedAddress=" + reapedAddress + '}';
        }
        
    }
    
}
