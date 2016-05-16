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
import cotton.network.DestinationMetaData;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
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
import java.util.ArrayList;
import java.io.Serializable;
import cotton.internalrouting.ServiceRequest;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author magnus
 * @author Tony
 */
public class LocalServiceDiscovery implements ServiceDiscovery {

    private SocketAddress localAddress;
    private InternalRoutingServiceDiscovery internalRouting;
    private AddressPool discoveryCache;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private ActiveServiceLookup localServiceTable = null;
    private ConcurrentHashMap<String, AddressPool> activeQueue;
    private ConcurrentHashMap<DestinationMetaData, AtomicInteger> destFailStat = new ConcurrentHashMap();
    private RequestQueueManager queueManager;
    private final ScheduledExecutorService taskScheduler;
    private final ScheduledThreadPoolExecutor deadAddressValidator;
    private ConcurrentLinkedQueue<ReapedAddress> deadAddresses;

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
                discoveryCache.addAddress(gAddr);
            }
        }
    }
    
    private void initGlobalDiscoveryPool(Configurator conf) {
        for(SocketAddress s: conf.getDiscoverySocketAddresses())
            System.out.println("local sockets: " + s);
        
        if (conf != null) {
            SocketAddress[] addrArr = conf.getDiscoverySocketAddresses();
            for (int i = 0; i < addrArr.length; i++) {
                DestinationMetaData gAddr = new DestinationMetaData(addrArr[i], PathType.DISCOVERY);
                discoveryCache.addAddress(gAddr);
            }
        }
    }

    public LocalServiceDiscovery(GlobalDiscoveryDNS dnsConfig) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(dnsConfig);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.activeQueue = new ConcurrentHashMap<>();
        this.taskScheduler = Executors.newScheduledThreadPool(5);
        this.deadAddresses = new ConcurrentLinkedQueue<>();
        deadAddressValidator = new ScheduledThreadPoolExecutor(1);
    }
    
    public LocalServiceDiscovery(Configurator conf) {
        this.discoveryCache = new AddressPool();
        initGlobalDiscoveryPool(conf);
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.activeQueue = new ConcurrentHashMap<>();
        this.taskScheduler= Executors.newScheduledThreadPool(5);
        deadAddressValidator = new ScheduledThreadPoolExecutor(1);
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
        if(dest == null){
            System.out.println("local, destinationUnreachable: dest is null");
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
                checkDeadAddresses(currentPool);
            }
            return newAddress;
        }
        // TODO: change to threashold over time

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
        DiscoveryProbe probe = new DiscoveryProbe(serviceName, null);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
        packet.setProbe(probe);

        DestinationMetaData dest = discoveryCache.getAddress();
        //DestinationMetaData dest = new DestinationMetaData(addr,PathType.DISCOVERY);
        try {
            byte[] data = serializeToBytes(packet);
            ServiceRequest request = internalRouting.sendWithResponse(dest, data, 400);
            if (request == null) {
                System.out.println("sendWithResponse (LocalServ: searchForService): " + serviceName + " Signal: " + signal);
                return RouteSignal.NOTFOUND;
            }
            if (request.getData() == null) {
                System.out.println("sendWithResponse (LocalServ: searchForService):request.getData() == null " + serviceName + " Signal: " + signal);
                System.out.println("\t" + this.discoveryCache.toString());
                return RouteSignal.NOTFOUND;
            }
            DiscoveryPacket discoveryPacket = packetUnpack(request.getData());
            DiscoveryProbe discoveryProbe = discoveryPacket.getProbe();
            DestinationMetaData destMeta = null;
            if (discoveryProbe != null && (destMeta = discoveryProbe.getAddress()) != null) {
                destination.setSocketAddress(destMeta.getSocketAddress());
                destination.setPathType(destMeta.getPathType());
                this.addService(dest, serviceName);
                return RouteSignal.NETWORKDESTINATION;
            }
        } catch (IOException ex) {
        }

        return signal;
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
            if (signal == RouteSignal.NOTFOUND) {
                System.out.println("Local: (getDestination):resolveOriginRoute-> NOTFOUND");
                return signal;
            }
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
//               AddressPool pool = this.serviceCache.get(serviceName);
//        if (pool == null) {
//            return searchForService(destination, serviceName);
//            }
//        AddressPool pool = this.activeQueue.get(serviceName);
//        DestinationMetaData tmp = null;
//        if(pool != null && (tmp = pool.getAddress()) != null){
//            signal = RouteSignal.NETWORKDESTINATION;
//            destination.setSocketAddress(tmp.getSocketAddress());
//            destination.setPathType(tmp.getPathType());
//            return signal;
//        }
        return searchForService(destination, serviceName);
//           DestinationMetaData addr = pool.getAddress();
//        if (addr != null) {
//            destination.setSocketAddress(addr.getSocketAddress());
//            destination.setPathType(addr.getPathType()); // default pathType for now
//            signal = RouteSignal.NETWORKDESTINATION;
//        }
//
//        return signal;
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
            System.out.println("Local: resolveOriginRoute: Origin null");
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

    private void processProbeRequest(Origin origin, DiscoveryProbe probe) {
        if (probe.getAddress() == null) {
            return;
        }
        if (probe.getAddress() != null) {
            if (origin.getAddress().equals(probe.getAddress().getSocketAddress()) && probe.getName() != null) {
                byte[] tmp = new byte[0];
                try {
                    tmp = serializeToBytes(new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE));
                }catch(IOException e){
                    e.printStackTrace();
                }
                internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, tmp);
            }
        }
        AddressPool pool = null;
        if (localServiceTable.getService(probe.getName()) == null) {
            probe.setAddress(null);
        }

        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE);
        packet.setProbe(probe);
        byte[] data = new byte[0];
        try {
            data = serializeToBytes(packet);
        } catch (IOException ex) {

        }
        internalRouting.sendBackToOrigin(origin, PathType.DISCOVERY, data);
    }

    private long announceDelay = 2;
    private ScheduledFuture<?> announceLaterTask = null;

    private void tryAnnounceLater() {
        announceDelay = (announceDelay < 10) ? announceDelay : 10;
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                if (announce()) {
                    System.out.println("Announce: done");
                } else {
                    System.out.println("Announce: failed");
                }

            }
        };

        System.out.println("try Announce in: " + announceDelay + " seconds again");

        final ScheduledFuture<?> schedule = this.taskScheduler.schedule(run, announceDelay, TimeUnit.SECONDS);
        ;
        this.taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                schedule.cancel(true);
            }
        }, announceDelay + 10, TimeUnit.SECONDS);
    }

    @Override
    public boolean announce() {
        if (localServiceTable == null) {
            System.out.println("Announce: no services to announce");
            return false;
        }
//        InetSocketAddress destAddr = (InetSocketAddress) discoveryCache.getAddress();
//        if(destAddr == null)
//            return false;
        DestinationMetaData dest = discoveryCache.getAddress();
        if (dest == null) {
            System.out.println("Announce: discovery addresses, check dns config");
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
        ConfigurationPacket configPacket = new ConfigurationPacket(localAddress, maxAmountOfQueues, totalServiceCapacity, false);
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
            if (!success) {
                tryAnnounceLater();
                //announceDelay++;
                return false;
            }
        } catch (IOException ex) {
            tryAnnounceLater();
            ex.printStackTrace();
            //announceDelay++;
            return false;
        }

        return true;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                System.out.println("FLAG VALUE" + flag);
            }
        }
    }

    private void checkDeadAddresses(AddressPool pool) {
        ConcurrentLinkedQueue<ReapedAddress> reapedAddresses = this.deadAddresses;

        InternalRoutingServiceDiscovery routing = this.internalRouting;
        Runnable command = new Runnable() {
            @Override
            public void run() {
                for (ReapedAddress addr : reapedAddresses) {
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
                    if (request != null || request.getData() != null) {
                        pool.addAddress(addr.getReapedAddress());
                    }
                }
            }
        };
        ScheduledFuture<?> addressValidator = this.deadAddressValidator.scheduleAtFixedRate(command, 10, 10, TimeUnit.SECONDS);
        this.deadAddressValidator.schedule(new Runnable() {
            public void run() {
                addressValidator.cancel(true);
            }
        }, 2 * 10 * 10, TimeUnit.SECONDS);
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

    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    @Override
    public void stop() {
        this.taskScheduler.shutdownNow();
        deadAddressValidator.shutdownNow();
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Handles updates messages from global discovery.
     *
     */
    @Override
    public void discoveryUpdate(Origin origin, byte[] data) {
        DiscoveryPacket packet = packetUnpack(data);
        decodeDiscoveryPacket(origin, packet);

    }

    @Override
    public void requestQueueMessage(DiscoveryPacket packet) {
        Origin origin = new Origin();
        origin.setAddress(localAddress);
        decodeDiscoveryPacket(origin, packet);
    }

    private void decodeDiscoveryPacket(Origin origin, DiscoveryPacket packet) {
        DiscoveryPacketType type = packet.getPacketType();
        //to do: switch not functioning properly with enums
        // System.out.println("DefaultGlobalServiceDiscovery: " + type
        //      + " from: " + ((InetSocketAddress) origin.getAddress()).toString());
        switch (type) {
            case DISCOVERYREQUEST:
                processProbeRequest(origin, packet.getProbe());
                break;
            case DISCOVERYRESPONSE:
                //localDiscovery.updateHandling(from, packet);
                break;
            case ANNOUNCE:
                //processAnnouncePacket(packet.getAnnounce());
                break;
            case REQUESTQUEUE:
                processQueuePacket(packet.getQueue());
                break;
            case CONFIG:
                processConfigPacket(packet.getConfigPacket());
                break;
            case CIRCUITBREAKER:
                triggeredCircuitBreaker(origin, packet.getCircuitBreakerPacket());
                break;
            default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
                System.out.println("DefaultGlobalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
                break;
        }
    }

    private DiscoveryPacket packetUnpack(byte[] data) {
        DiscoveryPacket probe = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (DiscoveryPacket) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(LocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }

    private void addService(DestinationMetaData addr, String name) {
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
        // TODO: implement logic redirecting services to their request queue
        DestinationMetaData sAddr = new DestinationMetaData(addr, PathType.SERVICE);
        for (String s : serviceList) {
            addService(sAddr, s);
        }
        printAnnounceList(serviceList);
    }

    private void addQueue(DestinationMetaData addr, String queue) {
        AddressPool pool = new AddressPool();
        AddressPool oldPool = this.activeQueue.putIfAbsent(queue, pool);
        if (oldPool != null) {
            oldPool.addAddress(addr);
        } else {
            pool.addAddress(addr);
        }
    }

    private void notifyQueue(int numInstances, String serviceName, DestinationMetaData address) {
        for (int i = 0; i < numInstances; i++) {
            this.internalRouting.notifyRequestQueue(address, RouteSignal.NETWORKDESTINATION, serviceName);
        }
    }

    private void triggeredCircuitBreaker(Origin origin, CircuitBreakerPacket circuit) {
        System.out.println("INCOMMING CIRCUITBREAKER MESSAGE");
        DestinationMetaData dest = discoveryCache.getAddress();
        if (dest == null) {
            return;
        }
        DiscoveryPacket discPacket = new DiscoveryPacket(DiscoveryPacketType.CIRCUITBREAKER);
        circuit.setInstanceAddress(localAddress);
        discPacket.setCircuitBreakerPacket(circuit);
        try {
            byte[] data = serializeToBytes(discPacket);
            internalRouting.sendToDestination(dest, data);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: logg errors
        }

    }

    private void processQueuePacket(QueuePacket packet) {
        if (packet == null) {
            System.out.println("ERROR in processQueuePacket");
            return;
        }
        String[] queueList = packet.getRequestQueueList();
        if (queueList == null) {
            System.out.println("queuelist is null in processQueuePacket");
            return;

        }
        SocketAddress addr = packet.getInstanceAddress();
        DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.REQUESTQUEUE);
        if (qAddr == null) {
            return;
        }
        for (String s : queueList) {
            addQueue(qAddr, s);
            ServiceMetaData service = this.localServiceTable.getService(s);
            if (service != null) {
                int num = service.getMaxCapacity() - service.getCurrentThreadCount();
                //this.internalrouting.notifyRequestQueue(qAddr, RouteSignal.NETWORKDESTINATION, s);
                notifyQueue(num, s, qAddr);
            }
        }
    }

    public boolean announceQueues(RequestQueueManager queueManager) {
        this.queueManager = queueManager;
        return true;
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

    @Override
    public boolean processCommand(Command command) {
        return false;
    }

    private void processConfigPacket(ConfigurationPacket packet) {
        if (packet == null) {
            return;
        }
        SocketAddress addr = packet.getInstanceAddress();
        ConfigEntry[] entry = packet.getConfigEntry();
        for (int i = 0; i < entry.length; i++) {
            ConfigEntry configEntry = entry[i];
            if (configEntry.getPathType() == PathType.REQUESTQUEUE) {
                DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.REQUESTQUEUE);
                addQueue(qAddr, configEntry.getName());
            } else if (configEntry.getPathType() == PathType.SERVICE) {
                DestinationMetaData qAddr = new DestinationMetaData(addr, PathType.SERVICE);
                addService(qAddr, configEntry.getName());
            }
        }
        if (packet.isGlobalServiceDiscovery()) {
            discoveryCache.addAddress(new DestinationMetaData(addr, PathType.DISCOVERY));
        }
    }

    private class ReapedAddress {

        private String name;
        private DestinationMetaData reapedAddress;

        public ReapedAddress(String name, DestinationMetaData reapedAddress) {
            this.name = name;
            this.reapedAddress = reapedAddress;
        }

        public String getName() {
            return this.name;
        }

        public DestinationMetaData getReapedAddress() {
            return this.reapedAddress;
        }
    }
}
