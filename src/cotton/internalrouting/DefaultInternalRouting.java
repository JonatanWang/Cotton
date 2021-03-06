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
import cotton.network.SocketLatch;
import java.net.SocketAddress;
import cotton.servicediscovery.RouteSignal;
import cotton.servicediscovery.ServiceDiscovery;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkPacket;
import cotton.network.DefaultServiceChain;
import cotton.network.NetworkHandler;
import cotton.services.BridgeServiceBuffer;
import cotton.services.ServiceBuffer;
import java.io.IOException;

import cotton.requestqueue.RequestQueueManager;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.systemsupport.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class DefaultInternalRouting implements InternalRoutingNetwork, InternalRoutingClient, InternalRoutingServiceDiscovery,
        InternalRoutingServiceHandler, InternalRoutingRequestQueue, StatisticsProvider {

    private NetworkHandler networkHandler;
    private SocketAddress localAddress;
    private ServiceDiscovery discovery;
    private ConcurrentHashMap<UUID, SocketLatch> keepAliveTable;
    private ConcurrentHashMap<UUID, ServiceRequest> connectionTable;
    //private ConcurrentLinkedQueue<NetworkPacket> routingQueue;
    private LinkedBlockingQueue<NetworkPacket> routingQueue;
    private ServiceBuffer serviceHandlerBridge;
    private RouteDispatcher dispatcher = null;
    private RequestQueueManager requestQueueManager = null;
    private Console commandConsole = null;
    private final ScheduledThreadPoolExecutor taskScheduler;

    public DefaultInternalRouting(NetworkHandler networkHandler, ServiceDiscovery discovery) {
        this.networkHandler = networkHandler;
        this.localAddress = networkHandler.getLocalAddress();
        System.out.println(localAddress);
        this.discovery = discovery;
        this.networkHandler.setInternalRouting(this);
        this.discovery.setNetwork(this, localAddress);
        this.keepAliveTable = new ConcurrentHashMap<>();
        this.connectionTable = new ConcurrentHashMap<>();
        //this.routingQueue = new ConcurrentLinkedQueue<>();
        this.routingQueue = new LinkedBlockingQueue<>();
        this.serviceHandlerBridge = new BridgeServiceBuffer();
        taskScheduler = new ScheduledThreadPoolExecutor(7);
        startTimeoutSchedule(50);

    }

    /**
     * initialiazes the RequestQueueManager
     *
     * @param requestQueueManager sets the requestQueueManager
     */
    public void setRequestQueueManager(RequestQueueManager requestQueueManager) {
        this.requestQueueManager = requestQueueManager;
        this.requestQueueManager.setInternalRouting(this);
        this.requestQueueManager.setLocalAddress(this.localAddress);
    }

    public void setCommandControl(Console commandConsole) {
        this.commandConsole = commandConsole;
    }

    /**
     * The InternalRoutingNetwork implementation
     */
    /**
     * adds a network packet to the routing queue to be processed
     *
     * @param networkPacket
     */
    @Override
    public void pushNetworkPacket(NetworkPacket networkPacket) {
        routingQueue.add(networkPacket);
    }

    /**
     * adds a network packet to the routing queue to be processed
     *
     * @param networkPacket
     */
    @Override
    public void pushKeepAlivePacket(NetworkPacket networkPacket, SocketLatch latch) {
        UUID latchID = UUID.randomUUID();
        if (keepAliveTable.putIfAbsent(latchID, latch) != null) {
            networkPacket.removeData();
            latch.setFailed(networkPacket);
            return;
        }
        networkPacket.getOrigin().setAddress(localAddress);
        networkPacket.getOrigin().setSocketLatchID(latchID);
        routingQueue.add(networkPacket);

        //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean fallBackSend(NetworkPacket packet, DestinationMetaData dest) {
        SocketAddress sockerAddr = null;
        if (dest == null || (sockerAddr = dest.getSocketAddress()) == null || packet == null) {
            return false;
        }
        try {
            networkHandler.send(packet, sockerAddr);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean fallbackSendKeepAlive(NetworkPacket packet, DestinationMetaData dest) {
        SocketAddress sockerAddr = null;
        if (dest == null || (sockerAddr = dest.getSocketAddress()) == null || packet == null) {
            return false;
        }
        try {
            this.networkHandler.sendKeepAlive(packet, sockerAddr);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * The InternalRoutingClient implementation
     */
    /**
     * Sends works to the service by using resolve destination to find where the
     * data is to be delivered
     *
     * @param data
     * @param serviceChain
     * @return
     */
    @Override
    public boolean sendToService(byte[] data, ServiceChain serviceChain) {
        Origin origin = new Origin();
        return resolveDestination(origin, serviceChain, data, false);
    }

    /**
     *
     * @param data
     * @param serviceChain
     * @return
     */
    @Override
    public ServiceRequest sendKeepAlive(byte[] data, ServiceChain serviceChain) {
        Origin origin = new Origin();
        ServiceRequest request = newServiceRequest(origin, 0);
        origin.setAddress(localAddress);
        if (resolveDestination(origin, serviceChain, data, true)) {
            return request;
        }

        this.removeServiceRequest(origin);
        return null;
    }

    /**
     * Sends client data to the cloud for processing and returns a request so
     * that the result can be retrieved.
     *
     * @param data a byte array of data to forward for processing
     * @param serviceChain
     */
    @Override
    public ServiceRequest sendWithResponse(byte[] data, ServiceChain serviceChain) {
        Origin origin = new Origin();
        ServiceRequest request = newServiceRequest(origin, 0);
        origin.setAddress(localAddress);
        if (resolveDestination(origin, serviceChain, data, false)) {
            return request;
        }
        this.removeServiceRequest(origin);
        return null;
    }

    /**
     * The InternalRoutingServiceDiscovery implementation
     */
    /**
     * SendBackToOrigin is used by ServiceDiscovery to send the data directly
     * back to the origin , without calling ServiceDiscovery again
     *
     * @param origin the origin
     * @param pathType not used
     * @param data
     * @return true if it succeeded
     */
    @Override
    public boolean sendBackToOrigin(Origin origin, PathType pathType, byte[] data) {
        NetworkPacket packet = prepareForTransmission(origin, null, data, pathType);
        try {
            //networkHandler.send(packet, origin.getAddress());
            networkHandler.send(packet, origin.getAddress());
            return true;
        } catch (IOException ex) {
            //TODO FIX
        }
        return false;
    }

    /**
     * Sends data internally within the cloud node.
     *
     * @param destination
     * @param route
     * @param data
     * @return
     */
    public boolean sendLocal(DestinationMetaData destination, RouteSignal route, byte[] data) {
        boolean success = false;
        if (RouteSignal.LOCALDESTINATION == route) {
            NetworkPacket packet = prepareForTransmission(new Origin(), null, data, destination.getPathType());
            routingQueue.add(packet);
            success = true;
        } else {
            sendToDestination(destination, data);
        }
        return success;
    }

    /**
     * Sends the data directly to destination without calling ServiceDiscovery
     * again
     *
     * @param dest the destination
     * @param data payload
     * @return
     */
    @Override
    public boolean sendToDestination(DestinationMetaData dest, byte[] data) {
        NetworkPacket packet = prepareForTransmission(new Origin(), null, data, dest.getPathType());
        try {
            //networkHandler.send(packet, dest.getSocketAddress());
            networkHandler.send(packet, dest.getSocketAddress());
            return true;
        } catch (IOException ex) {
            //TODO Fix
        }
        return false;
    }
    
    /**
     * Sends data to a given destination and use serviceChain. 
     * @param dest
     * @param serviceChain
     * @param data
     * @return 
     */
    @Override
    public boolean sendToDestination(DestinationMetaData dest,ServiceChain serviceChain,byte[] data) {
        NetworkPacket packet = prepareForTransmission(new Origin(), serviceChain, data, dest.getPathType());
        try {
            //networkHandler.send(packet, dest.getSocketAddress());
            networkHandler.send(packet, dest.getSocketAddress());
            return true;
        } catch (IOException ex) {
            //TODO Fix
        }
        return false;
    }
    

    /**
     * Sends the data directly to destination without calling ServiceDiscovery
     * again
     *
     * @param dest destination
     * @param data
     * @return ServiceRequest
     */
    @Override
    public ServiceRequest sendWithResponse(DestinationMetaData dest, byte[] data, int timeout) {
        Origin origin = new Origin();
        origin.setAddress(this.localAddress);
        ServiceRequest request = newServiceRequest(origin, timeout);
        
        NetworkPacket packet = prepareForTransmission(origin, null, data, dest.getPathType());
        try {
            //networkHandler.send(packet, dest.getSocketAddress());
            networkHandler.send(packet, dest.getSocketAddress());
        } catch (IOException ex) {
            System.out.println("InternalRoutingServiceDiscovery:sendWithResponse: send fail");
            removeServiceRequest(origin);
            return null;
            // TODO Logging
        }

        return request;

    }

    /**
     * Notifies the requestQueue that this instance is available
     *
     * @param serviceName a serviceName to find a given queue by.
     */
    @Override
    public boolean notifyRequestQueue(DestinationMetaData destination, RouteSignal route, String serviceName) {
        // TODO: actually notify the queue
        destination.setPathType(PathType.REQUESTQUEUEUPDATE);
        Origin origin = new Origin();
        origin.setAddress(this.localAddress);
        byte[] data = serviceName.getBytes(StandardCharsets.UTF_8);
        //String tt = new String(data,StandardCharsets.UTF_8);
        //System.out.println("ServiceDiscovery::notifyRequestQueue:" + tt);
        NetworkPacket packet = prepareForTransmission(origin, null, data, destination.getPathType());
        if (route == RouteSignal.LOCALDESTINATION) {
            routingQueue.add(packet);
        } else if (route == RouteSignal.NETWORKDESTINATION) {
            try {
                //networkHandler.send(packet,destination.getSocketAddress());
                networkHandler.send(packet, destination.getSocketAddress());
            } catch (IOException e) {
                // TODO: logging
                destination = discovery.destinationUnreachable(destination, serviceName);
                fallBackSend(packet, destination);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * The InternalRoutingServiceHandler implementation
     */
    /**
     * Forward the result from the serviceHandler to the next link in the chain
     *
     * @param origin packet for this requestChain
     * @param serviceChain the chain containing the rest of the chain
     * @param result result from serviceHandler
     * @return false if it failed to send the data
     */
    @Override
    public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result) {
        return resolveDestination(origin, serviceChain, result, false);
    }

    @Override
    public ServiceBuffer getServiceBuffer() {
        return serviceHandlerBridge;
    }

    /**
     * Notifies the requestQueue that this instance is available
     *
     * @param serviceName a serviceName to find a given queue by.
     */
    @Override
    public boolean notifyRequestQueue(String serviceName) {
        // TODO: actually notify the queue
        DestinationMetaData destination = new DestinationMetaData();
        RouteSignal route = discovery.getRequestQueueDestination(destination, serviceName);
        destination.setPathType(PathType.REQUESTQUEUEUPDATE);
        Origin origin = new Origin();
        origin.setAddress(this.localAddress);
        byte[] data = serviceName.getBytes(StandardCharsets.UTF_8);
        //String tt = new String(data,StandardCharsets.UTF_8);
        //System.out.println("ServiceHandler::notifyRequestQueue:" + tt);
        NetworkPacket packet = prepareForTransmission(origin, null, data, destination.getPathType());
        if (route == RouteSignal.LOCALDESTINATION) {
            routingQueue.add(packet);
        } else if (route == RouteSignal.NETWORKDESTINATION) {
            try {
                //networkHandler.send(packet,destination.getSocketAddress());
                networkHandler.send(packet, destination.getSocketAddress());
            } catch (IOException e) {
                // TODO: logging
                return false;
            }
        }
        return true;
    }

    /**
     * InternalRoutingQueue methods
     */
    @Override
    public void sendWork(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        networkHandler.send(netPacket, dest);
    }

    @Override
    public void notifyDiscovery(DiscoveryPacket packet) {
        discovery.requestQueueMessage(packet);
    }

    /**
     * The InternalRouting helper methods implementation
     */
    /**
     * This Creates a new service request and register it in the system, (fills
     * in the origin) This need to be matched with removeServiceRequest on the
     * other end
     *
     * @param origin field for service request is filled in
     * @return ServiceRequest that can be used by this system
     */
    public ServiceRequest newServiceRequest(Origin origin, int timeout) {
        UUID requestID = UUID.randomUUID();
        origin.setServiceRequestID(requestID);
        ServiceRequest requestLatch;
        long timeStamp = System.currentTimeMillis() + timeout;
        if (timeout == 0) {
            timeStamp = 0;
        }
        requestLatch = new DefaultServiceRequest(timeStamp);
        if (connectionTable.putIfAbsent(requestID, requestLatch) != null) {
            return null;
        }
        if (timeStamp != 0) {
            //scheduleTask(timeStamp);
        }
        return requestLatch;
    }
    private long nextTime = 0;

    private AtomicBoolean timeoutSchedule = new AtomicBoolean(true);
    private ConcurrentLinkedQueue<Long> timeoutschedule = new ConcurrentLinkedQueue<>();
    /**
     * Schedules threads sets a reaper function to repeat every checkInterval
     * timestamp.
     *
     * @param checkInterval the length between check
     */
    public void startTimeoutSchedule(long checkInterval) {
        final long checkInterval1 = checkInterval;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (timeoutSchedule.get()) {
                    reapTimedOutRequest();
                    try {
                        Thread.sleep(checkInterval1);
                    } catch (InterruptedException ex) {}
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    private void reapTimedOutRequest() {
        long time = System.currentTimeMillis();
        DefaultServiceRequest req;
        ArrayList<UUID> reapedServiceRequest = new ArrayList<>();
        for (Map.Entry<UUID, ServiceRequest> entry : connectionTable.entrySet()) {
            req = (DefaultServiceRequest) entry.getValue();
            if ((time - req.getTimeStamp()) > 0 && req.getTimeStamp() != 0) {
                req.setFailed("SocketRequest timed out ");
                reapedServiceRequest.add(entry.getKey());
            }
        }
        for (UUID id : reapedServiceRequest) {
            connectionTable.remove(id);
        }

    }

    /**
     * The other end of newServiceRequest, gets back the ServiceRequest that
     * origin was associated with
     *
     * @param origin that has the ServiceRequest id
     * @return ServiceRequest
     */
    private ServiceRequest removeServiceRequest(Origin origin) {
        if (origin.getServiceRequestID() == null) {
            return null;
        }
        return connectionTable.remove(origin.getServiceRequestID());
    }

    /**
     * @param origin The source who inititated the service request
     * @param path The path for the networkpacket.
     * @param data A byte array of data
     * @param pathType The destination type of the sub system.
     * @return A filled in networkpacket ready for transmission.
     *
     */
    private NetworkPacket prepareForTransmission(Origin origin, ServiceChain path, byte[] data, PathType pathType) {
        if (path == null) {
            path = new DefaultServiceChain();
        }
        NetworkPacket packet = NetworkPacket.newBuilder()
                .setData(data)
                .setOrigin(origin)
                .setPath(path)
                .setPathType(pathType)
                .build();

        return packet;
    }

    /**
     * Forwads the data to correct destination.
     *
     * @param origin The source who inititated the service request
     * @param serviceChain The serviceChain
     * @param data A byte array of data
     * @param keepAlive A boolean whether the service request needs a keep alive
     * socket or not.
     * @return boolean if it is succesfully routed or not.
     *
     */
    private boolean resolveDestination(Origin origin, ServiceChain serviceChain, byte[] data, boolean keepAlive) {
        boolean success = false;
        DestinationMetaData dest = new DestinationMetaData();
        RouteSignal route = discovery.getDestination(dest, origin, serviceChain);
        NetworkPacket packet = null;
        switch (route) {
            case LOCALDESTINATION:
                packet = prepareForTransmission(origin, serviceChain, data, dest.getPathType());
                routingQueue.add(packet);
                success = true;
                break;
            case NETWORKDESTINATION:
                packet = prepareForTransmission(origin, serviceChain, data, dest.getPathType());
                if (keepAlive) {
                    try {
                        this.networkHandler.sendKeepAlive(packet, dest.getSocketAddress());
                        success = true;
                    } catch (IOException e) {
                        dest = discovery.destinationUnreachable(dest, serviceChain.peekNextServiceName());
                        success = fallbackSendKeepAlive(packet, dest);
                        e.printStackTrace();
                    }
                } else {
                    try {
                        this.networkHandler.send(packet, dest.getSocketAddress());
                        success = true;
                    } catch (IOException e) {
                        dest = discovery.destinationUnreachable(dest, serviceChain.peekNextServiceName());
                        success = fallBackSend(packet, dest);
                        e.printStackTrace();
                    }
                }
                break;
            case BRIDGELATCH:
                packet = prepareForTransmission(origin, serviceChain, data, dest.getPathType());
                SocketLatch socketLatch = keepAliveTable.get(origin.getSocketLatchID());
                if (socketLatch == null) {
                    System.out.println("SocketLatch not found");
                    //TODO: log error
                } else {
                    socketLatch.setData(packet);
                    success = true;
                }
                break;
            case RETURNTOORIGIN:
                packet = prepareForTransmission(origin, serviceChain, data, dest.getPathType());
                try {
                    //this.networkHandler.send(packet, origin.getAddress());
                    this.networkHandler.send(packet, origin.getAddress());
                    success = true;
                } catch (IOException ex) {
                    //TODO Fix
                    ex.printStackTrace();
                }
                break;
            case ENDPOINT:
                DefaultServiceRequest request = (DefaultServiceRequest) removeServiceRequest(origin);
                if (request != null) {
                    request.setData(data);
                    success = true;
                }
                break;
            case NOTFOUND:
                // sendWithResponse (LocalServ: searchForService):request.getData() == null mathpow2 Signal: NOTFOUND
                // happens when a node is overwhelmed and start droping connections, if this happens to often its
                // address is removed from the address pool, and if the pool goes empty then the above happens.
                // TODO: 
                System.out.println("resolveDestination: NOTFOUND");
                break;

            default:
                System.out.println("Stuff is not working");
                break;
            //TODO: implement error/logging
        }
        return success;
    }

    /**
     * starts a routing dispatcher thread.
     */
    public void start() {
        this.dispatcher = new RouteDispatcher();
        Thread th = new Thread(this.dispatcher);
        th.setDaemon(true);
        th.start();
        if (requestQueueManager != null) {
            String[] nameList = requestQueueManager.getActiveQueues();
            discovery.announceQueues(requestQueueManager);
            System.out.println("REQUEST QUEUE NAMELIST: " + Arrays.toString(nameList));
        }

    }

    /**
     * sends a stop signal for the routing dispatcher thread
     */
    public void stop() {
        if(this.requestQueueManager != null)
            this.requestQueueManager.stop();
        if(this.taskScheduler != null)
            this.taskScheduler.shutdownNow();
        dispatcher.stop();
    }

    /**
     * StatisticsProvider
     *
     *
     */

    @Override
    public StatisticsData[] getStatisticsForSubSystem(String name) {
        return new StatisticsData[0];
    }

    @Override
    public StatisticsData getStatistics(String[] name) {
        return new StatisticsData();
    }
    /**
     * returns a statistics provider that can be asked for statistics.
     *
     * @return
     */
    @Override
    public StatisticsProvider getProvider() {
        return this;
    }
    /**
     * returns what the component type that the statistics were requested for.
     *
     * @return
     */
    @Override
    public StatType getStatType() {
        return StatType.INTERNALROUTING;
    }
    /**
     * processes a command from the command and control unit.
     * @param command
     * @return
     */
    @Override
    public StatisticsData[] processCommand(Command command) {
        return new StatisticsData[0];
    }

    private class RouteDispatcher implements Runnable {

        private volatile boolean running = false;

        /**
         * starts the thread and dispatches networkpackets in the queue.
         */
        @Override
        public void run() {
            running = true;
            while (running) {
                //NetworkPacket packet = routingQueue.poll();
                NetworkPacket packet = null;
                try {
                    packet = routingQueue.take();
                } catch (InterruptedException ex) {
                    Logger.getLogger(DefaultInternalRouting.class.getName()).log(Level.SEVERE, null, ex);
                    
                }
                if (packet == null) {
//                    try {
//                        Thread.sleep(5);
//                    } catch (InterruptedException ex) {
//
//                    }
                } else {
                    processPacket(packet);
                }
            }
        }

        /**
         * Stops the current thread.
         */
        public void stop() {
            running = false;
        }

        /**
         * Routing internal incoming network packet to their final destinations.
         *
         * @param packet The networkpacket from the routing Queue
         */
        private void processPacket(NetworkPacket packet) {
            RouteSignal signal = discovery.getLocalInterface(packet.getOrigin(), packet.getPath());

            if (signal == RouteSignal.ENDPOINT) {
                DefaultServiceRequest request = (DefaultServiceRequest) removeServiceRequest(packet.getOrigin());
                if(request == null) {
                    // TODO: logg request removed
                    return;
                }
                if(packet.getData() == null) {
                    request.setFailed("ENDPOINT failure no data returned");
                    return;
                }
                request.setData(packet.getData());
                return;
            } else if (signal == RouteSignal.NETWORKDESTINATION) {
                forwardResult(packet.getOrigin(), packet.getPath(), packet.getData());
                return;
            } else if (signal == RouteSignal.BRIDGELATCH) {
                UUID latchID = packet.getOrigin().getSocketLatchID();
                SocketLatch latch = keepAliveTable.get(latchID);
                if (latch != null) {
                    latch.setData(packet);
                }
                return;
            }

            String serviceName = null;
            switch (packet.getType()) {
                case RELAY:
                    break;
                case COMMANDCONTROL:
                    if (commandConsole != null) {
                        commandConsole.processCommand(packet);
                    }
                    break;
                case DISCOVERY:
                    discovery.discoveryUpdate(packet.getOrigin(), packet.getData());
                    break;
                case SERVICE:
                    //ServicePacket servicePacket = new ServicePacket(packet.getOrigin(), packet.getData(), packet.getPath());
                    serviceHandlerBridge.add(packet);
                    break;
                case REQUESTQUEUE:
                    if (requestQueueManager == null) {
                        break; // TODO: give error
                    }
                    ServiceChain pathChain = packet.getPath();
                    if (pathChain != null && (serviceName = pathChain.peekNextServiceName()) != null) {
                        requestQueueManager.queueService(packet, serviceName);
                    }
                    break;
                case REQUESTQUEUEUPDATE:
                    byte[] data = packet.getData();
                    if (requestQueueManager == null) {
                        break; // TODO: give error
                    }
                    serviceName = new String(data, StandardCharsets.UTF_8);
                    requestQueueManager.addAvailableInstance(packet.getOrigin(), serviceName);
                    break;
                case UNKNOWN:
                    System.out.println("PacketType unknown in process packet");
                    break;
                case NOTFOUND:
                    System.out.println("PacketType NOT found in process packet");
                    break;
                default:
                    System.out.println("PacketType invalid in process packet");
                    //TODO: logg error
                    break;
            }
        }
    }

}
