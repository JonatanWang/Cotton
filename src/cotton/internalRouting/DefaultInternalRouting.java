package cotton.internalRouting;

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
import cotton.network.DummyServiceChain;
import cotton.network.NetworkHandler;
import cotton.services.BridgeServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServicePacket;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class DefaultInternalRouting implements InternalRoutingNetwork, InternalRoutingClient, InternalRoutingServiceDiscovery, InternalRoutingServiceHandler {

    private NetworkHandler networkHandler;
    private SocketAddress localAddress;
    private ServiceDiscovery discovery;
    private ConcurrentHashMap<UUID, SocketLatch> keepAliveTable;
    private ConcurrentHashMap<UUID, ServiceRequest> connectionTable;
    private ConcurrentLinkedQueue<NetworkPacket> routingQueue;
    private ServiceBuffer serviceHandlerBridge;
    private RouteDispatcher dispatcher = null;

    public DefaultInternalRouting(NetworkHandler networkHandler, ServiceDiscovery discovery) {
        this.networkHandler = networkHandler;
        this.localAddress = networkHandler.getLocalAddress();
        this.discovery = discovery;
        this.networkHandler.setInternalRouting(this);
        this.discovery.setNetwork(this, localAddress);
        this.keepAliveTable = new ConcurrentHashMap<>();
        this.connectionTable = new ConcurrentHashMap<>();
        this.routingQueue = new ConcurrentLinkedQueue<>();
        this.serviceHandlerBridge = new BridgeServiceBuffer();
    }

    /**
     * The InternalRoutingNetwork implementation
     */
    /**
     *
     * @param networkPacket
     */
    @Override
    public void pushNetworkPacket(NetworkPacket networkPacket) {
        routingQueue.add(networkPacket);
    }

    /**
     *
     * @param networkPacket
     * @param latch
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

    /**
     * The InternalRoutingClient implementation
     */
    /**
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
        ServiceRequest request = newServiceRequest(origin);
        origin.setAddress(localAddress);
        if (resolveDestination(origin, serviceChain, data, true)) {
            return request;
        }

        this.removeServiceRequest(origin);
        return null;
    }

    @Override
    public ServiceRequest sendWithResponse(byte[] data, ServiceChain serviceChain) {
        Origin origin = new Origin();
        ServiceRequest request = newServiceRequest(origin);
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
    public boolean SendBackToOrigin(Origin origin, PathType pathType, byte[] data) {
        NetworkPacket packet = prepareForTransmission(origin, null, data, pathType);
        try {
            networkHandler.send(packet, origin.getAddress());
            return true;
        } catch (IOException ex) {
            //TODO FIX
        }
        return false;
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
    public boolean SendToDestination(DestinationMetaData dest, byte[] data) {
        NetworkPacket packet = prepareForTransmission(new Origin(), null, data, dest.getPathType());
        try {
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
    public ServiceRequest sendWithResponse(DestinationMetaData dest, byte[] data) {
        Origin origin = new Origin();
        ServiceRequest request = newServiceRequest(origin);
        origin.setAddress(this.localAddress);

        NetworkPacket packet = prepareForTransmission(origin, null, data, dest.getPathType());
        try {
            networkHandler.send(packet, dest.getSocketAddress());
        } catch (IOException ex) {
            removeServiceRequest(origin);
            return null;
            // TODO Logging
        }
        
        return request;

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
    private ServiceRequest newServiceRequest(Origin origin) {
        UUID requestID = UUID.randomUUID();
        origin.setServiceRequestID(requestID);
        ServiceRequest requestLatch = new DefaultServiceRequest();
        if (connectionTable.putIfAbsent(requestID, requestLatch) != null) {
            return null;
        }
        return requestLatch;
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
     * @param serviceChain The serviceChain , if null a empty serviceChain is
     * created
     * @param data A byte array of data
     * @param keepAlive A boolean whether the service request needs a keep alive
     * socket or not.
     * @param pathType The destination type of the sub system.
     * @return A filled in networkpacket ready for transmission.
     *
     */
    private NetworkPacket prepareForTransmission(Origin origin, ServiceChain path, byte[] data, PathType pathType) {
        if (path == null) {
            path = new DummyServiceChain();
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
                try {
                    if (keepAlive) {
                        this.networkHandler.sendKeepAlive(packet, dest.getSocketAddress());
                    } else {
                        this.networkHandler.send(packet, dest.getSocketAddress());
                    }
                    success = true;
                } catch(IOException e) {
                    //TODO Fix
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
                    this.networkHandler.send(packet, origin.getAddress());
                    success = true;
                } catch (IOException ex) {
                    //TODO Fix
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
        new Thread(this.dispatcher).start();
    }

    /**
     * sends a stop signal for the routing dispatcher thread
     */
    public void stop() {
        dispatcher.stop();
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
                NetworkPacket packet = routingQueue.poll();
                if (packet == null) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {

                    }
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
                request.setData(packet.getData());
                return;
            } else if (signal == RouteSignal.NETWORKDESTINATION) {
                forwardResult(packet.getOrigin(), packet.getPath(), packet.getData());
                return;
            } else if (signal == RouteSignal.BRIDGELATCH) {
                UUID latchID = packet.getOrigin().getSocketLatchID();
                SocketLatch latch = keepAliveTable.get(latchID);
                if(latch != null) {
                    latch.setData(packet);
                }
                return;
            }

            switch (packet.getType()) {
                case RELAY:
                    break;
                case DISCOVERY:
                    discovery.discoveryUpdate(packet.getOrigin(), packet.getData());
                    break;
                case SERVICE:
                    ServicePacket servicePacket = new ServicePacket(packet.getOrigin(), packet.getData(), packet.getPath());
                    serviceHandlerBridge.add(servicePacket);
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
