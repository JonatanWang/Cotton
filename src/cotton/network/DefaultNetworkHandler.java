package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cotton.servicediscovery.RouteSignal;
import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServicePacket;
import cotton.network.NetworkPacket;
import cotton.servicediscovery.ServiceDiscovery;

/**
 * Handles all of the packet buffering and relaying.
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public class DefaultNetworkHandler implements NetworkHandler,ClientNetwork {
    private ServiceBuffer serviceBuffer;
    private ConcurrentHashMap<UUID,DefaultServiceRequest> connectionTable;
    private AtomicBoolean running;
    private ServiceDiscovery localServiceDiscovery;
    private int localPort;
    private InetAddress localIP;

    public DefaultNetworkHandler(ServiceDiscovery localServiceDiscovery) throws java.net.UnknownHostException{
        this.localPort = 3333; // TODO: Remove hardcode on port
        try{
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }
        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this, getLocalSocketAddress()); // TODO: get socket address
    }

    /**
     * Send a serializable piece of data to the specified destination
     *
     * @param data The data to send.
     * @param destination The information about where the packet is headed.
     * @return Whether the connection succeeded or not
     */
    @Override
    public boolean send(Serializable data, ServiceConnection destination) {
        Socket socket = new Socket(); // TODO: Encryption
        try {
            socket.connect(destination.getAddress());
            new ObjectOutputStream(socket.getOutputStream()).writeObject(data);
            return true;
        }catch (IOException e) {
            logError("send: " + e.getMessage());
            return false;
        }finally{
            try {
                socket.close();
            } catch (Throwable e) {
                logError("send socket close: " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public ServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException{
        Socket socket = new Socket(); // TODO: Encryption
        UUID uuid = UUID.randomUUID();
        DefaultServiceRequest request = new DefaultServiceRequest();
        try {
            socket.connect(destination.getAddress());
            new ObjectOutputStream(socket.getOutputStream()).writeObject(data);
            this.connectionTable.put(uuid, request);
            return request;
        }catch (IOException e) {
            logError("send: " + e.getMessage());
            throw e;
        }finally{
            try {
                socket.close();
            } catch (Throwable e) {
                logError("send socket close: " + e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public void sendToService(Serializable data, ServiceChain path, ServiceConnection from) {
        ServiceConnection dest = new DefaultServiceConnection();
        RouteSignal route = localServiceDiscovery.getDestination(dest, from, path);

        if(path.peekNextServiceName() == null && from != null) {
            DefaultServiceRequest req = connectionTable.get(from.getUserConnectionId());
            if(req == null) return; // TODO: dont drop results, and send data to service discovary
            req.setData(data);
        }

        if(route == RouteSignal.LOCALDESTINATION) {
            sendToServiceBuffer(from, data, path);
            return;
        }else if (route == RouteSignal.NOTFOUND){
            return;
        }

        data = buildServicePacket(data, path, from);

        send(data, dest);
    }


    @Override
    public ServiceRequest sendToService(Serializable data, ServiceChain path) { // TODO: Make sure that this doesn't drop packages with this as last destination
        UUID uuid = UUID.randomUUID();
        ServiceConnection dest = new DefaultServiceConnection(uuid);
        RouteSignal route = localServiceDiscovery.getDestination(dest, path);
        DefaultServiceRequest result = new DefaultServiceRequest();
        if(route == RouteSignal.LOCALDESTINATION) {
            sendToServiceBuffer(dest, data, path);
            this.connectionTable.put(dest.getUserConnectionId(), result);
            return result;
        }

        data = buildServicePacket(data, path, getLocalServiceConnection());

        if(send(data,dest)){
            this.connectionTable.put(uuid, result);
            return result;
        }
        return null;
    }

    @Override
    public void run(){
        ServerSocket encryptedServerSocket = null;
        try{
            encryptedServerSocket = new ServerSocket();
        }catch(IOException e){
            e.printStackTrace();
        }
        while(running.get()==true){
            //TODO: Fix networking
        }
    }

    @Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    private void sendToServiceBuffer(ServiceConnection from, Serializable data, ServiceChain path) {
        ServicePacket servicePacket = null;
        try{
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            objectStream.writeObject(data);
            objectStream.flush();
            objectStream.close();

            InputStream in = new ByteArrayInputStream(byteStream.toByteArray());

            servicePacket = new ServicePacket(from, in, path);
        }catch(IOException e){
            e.printStackTrace();
        }
        serviceBuffer.add(servicePacket);
    }

    private NetworkPacket buildServicePacket(Serializable data, ServiceChain path, ServiceConnection from){
        return new NetworkPacket(data, path, from, NetworkPacket.PacketType.SERVICE);
    }

    private ServiceConnection getLocalServiceConnection(){
        SocketAddress address = getLocalSocketAddress();
        ServiceConnection local = new DefaultServiceConnection();
        return local;
    }

    private SocketAddress getLocalSocketAddress(){
        SocketAddress address = new InetSocketAddress(localIP, localPort);
        return address;
    }

    private void logError(String error){
        System.out.println("Network exception, " + error); // TODO: MAKE THIS ACTUALLY LOG
    }

}
