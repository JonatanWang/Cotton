package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService threadPool;
    private SocketAddress localSocketAddress;

    public DefaultNetworkHandler(ServiceDiscovery localServiceDiscovery) throws java.net.UnknownHostException{
        if(localServiceDiscovery == null)
            throw new NullPointerException("Recieved null servicediscovery");

        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this, getLocalSocketAddress()); // TODO: get socket address
        running = new AtomicBoolean(true);
    }

    public DefaultNetworkHandler(ServiceDiscovery localServiceDiscovery,SocketAddress localSocketAddress) throws java.net.UnknownHostException{
        if(localServiceDiscovery == null)
            throw new NullPointerException("Recieved null servicediscovery");

        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this, getLocalSocketAddress()); // TODO: get socket address
        running = new AtomicBoolean(true);
        this.localSocketAddress = localSocketAddress;
    }

    public DefaultNetworkHandler(ServiceDiscovery localServiceDiscovery,int port) throws java.net.UnknownHostException{
        if(localServiceDiscovery == null)
            throw new NullPointerException("Recieved null servicediscovery");

        this.localPort = port; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this, getLocalSocketAddress()); // TODO: get socket address
        running = new AtomicBoolean(true);
        
    }

    /**
     * Send a serializable piece of data to the specified destination
     *
     * @param data The object to send.
     * @param destination The information about where the packet is headed.
     * @return Whether the connection succeeded or not.
     */
    private boolean sendObject(Serializable data, ServiceConnection destination) {
        if(data == null) throw new NullPointerException("Null data");
        if(destination == null) throw new NullPointerException("Null destination");

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

    /**
     * Send a serializable piece of data to the specified destination
     *
     * @param data The object to send.
     * @param destination The information about where the packet is headed.
     * @return Whether the connection succeeded or not.
     */
    @Override
    public boolean send(Serializable data, ServiceConnection destination) {
        data = buildServicePacket(data, null, getLocalServiceConnection(destination.getUserConnectionId()), destination.getPathType());
        return sendObject(data, destination);
    }

    /**
     * Sends a serializable object to the specified destination, returning a response.
     *
     * @param data The object to send.
     * @param destination The information about where the packet is headed.
     * @return The result with which the response will be returned.
     */
    @Override
    public ServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException{
        DefaultServiceRequest result = new DefaultServiceRequest();
        ServiceConnection local = getLocalServiceConnection();
        NetworkPacket packet = buildServicePacket(data, null, local, destination.getPathType());
        if(sendObject(packet, destination)){
            this.connectionTable.put(local.getUserConnectionId(), result);
            return result;
        }
        return null;
    }

    /**
     * Sends a request to a service, destination is decided by the servicediscovery
     *
     * @param data The object to send.
     * @param path The predefined services to pass through.
     * @param from The origin of this request.
     */
    @Override
    public void sendToService(Serializable data, ServiceChain path, ServiceConnection from) {
        ServiceConnection dest = new DefaultServiceConnection();
        RouteSignal route = localServiceDiscovery.getDestination(dest, from, path);

        switch(route){
        case LOCALDESTINATION:
            sendToServiceBuffer(from, data, path);
            return;
        case ENDPOINT:
            DefaultServiceRequest req = connectionTable.get(from.getUserConnectionId());
            if(req == null) {
/*                System.out.println("Network route table error, ENDPOINT " 
                        +"\n\tfrom: " + from.getUserConnectionId()
                        +"\n\tdest: " + dest.getUserConnectionId());
      */          
                return;
            } // TODO: dont drop results, and send data to service discovary
            req.setData(data);
            return;
        case RETURNTOORIGIN:
            // TODO: implement this
            break;
        case NOTFOUND:
            
            return;
        }

        data = buildServicePacket(data, path, from, dest.getPathType());

        sendObject(data, dest);
    }

    /**
     * Sends a request to a service, destination is decided by the servicediscovery with this node as origin.
     *
     * @param data The object to send.
     * @param path The predefined services to pass through.
     */
    @Override
    public ServiceRequest sendToService(Serializable data, ServiceChain path) { // TODO: Make sure that this doesn't drop packages with this as last destination
        UUID uuid = UUID.randomUUID();
        ServiceConnection dest = new DefaultServiceConnection(uuid);
        RouteSignal route = localServiceDiscovery.getDestination(dest, path);
        DefaultServiceRequest result = new DefaultServiceRequest();
        this.connectionTable.put(dest.getUserConnectionId(), result);
        if(route == RouteSignal.LOCALDESTINATION) {
            sendToServiceBuffer(dest, data, path);
            return result;
        }
        if(route == RouteSignal.RETURNTOORIGIN) {
             // TODO: implement this
        }
           
        data = buildServicePacket(data, path, getLocalServiceConnection(dest.getUserConnectionId()), dest.getPathType());

        if(sendObject(data,dest)){
            return result;
        }
        return null;
    }

    @Override
    public void run(){
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket();
            serverSocket.bind(getLocalSocketAddress());
            serverSocket.setSoTimeout(80);
        }catch(IOException e){// TODO: Logging
            e.printStackTrace();
        }

        Socket clientSocket = null;

        while(running.get() == true){
            try {
                if( (clientSocket = serverSocket.accept()) != null ){
                    NetworkUnpacker pck = new NetworkUnpacker(clientSocket);
                    threadPool.execute(pck);
                }
            }catch(SocketTimeoutException ignore){
            }catch (Throwable e) {
                System.out.println("Error " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            threadPool.shutdown();
            serverSocket.close();
        }catch (Throwable e) { //TODO EXCEPTION HANDLING
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Returns the next package in the serviceQueue.
     *
     * @return The next package.
     */
    @Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    /**
     * Stops the network-thread.
     */
    public void stop(){
        running.set(false);
    }

    private InputStream serializableToInputStream(Serializable data){
        ServicePacket servicePacket = null;
        InputStream in = null;
        try{
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            objectStream.writeObject(data);
            objectStream.flush();
            objectStream.close();

            in = new ByteArrayInputStream(byteStream.toByteArray());
        }catch(IOException e){
            e.printStackTrace();
        }
        return in;
    }
/**
 * Sends the data to a local service waiting on the data , puts it in the right ServiceRequest, 
 * @param data the result of a client ServiceRequest
 * @param destination
 * @return 
 */
    public boolean sendEnd(Serializable data, ServiceConnection destination) {
        DefaultServiceRequest req = this.connectionTable.get(destination.getUserConnectionId());
        if(req == null) return false;
        req.setData(data);
        return true;
    }
    
    private void sendToServiceBuffer(ServiceConnection from, Serializable data, ServiceChain path) {
        ServicePacket servicePacket = new ServicePacket(from, serializableToInputStream(data), path);
        serviceBuffer.add(servicePacket);
    }

    private NetworkPacket buildServicePacket(Serializable data, ServiceChain path, ServiceConnection from, PathType type){
        return new DefaultNetworkPacket(data, path, from, type);
    }
/**
 * Give back a new ServiceConnection but keeps the uuid between jumps
 * This is needed to prevent a huge bug when the uuid gets lost between machines
 * @param uuid the uuid the new ServiceConnection should contain
 * @return 
 */
    private ServiceConnection getLocalServiceConnection(UUID uuid){
        SocketAddress address = getLocalSocketAddress();
        ServiceConnection local = new DefaultServiceConnection(uuid);
        local.setAddress(address);
        return local;
    }
    /**
     * getLocalServiceConnection
     * Warning: improper use of this method can lead to huge network routing bugs,
     *      related to losing the uuid between jumps, use the above method instead
     * @return ServiceConnection with the local socket
     */
    private ServiceConnection getLocalServiceConnection(){
        SocketAddress address = getLocalSocketAddress();
        ServiceConnection local = new DefaultServiceConnection();
        local.setAddress(address);
        return local;
    }

    private SocketAddress getLocalSocketAddress(){
        SocketAddress address = new InetSocketAddress(localIP, localPort);
        return address;
    }

    private void logError(String error){
        System.out.println("Network exception, " + error); // TODO: MAKE THIS ACTUALLY LOG
    }

    private class NetworkUnpacker implements Runnable {
        Socket clientSocket = null;

        public NetworkUnpacker(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            System.out.println("Incoming connection to: " + clientSocket.getLocalSocketAddress() +" from" + clientSocket.getRemoteSocketAddress());
            try {
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                NetworkPacket input = (NetworkPacket)in.readObject();
                //System.out.println("Socket closed, parsing packet!");
                if(input == null) {
                    System.out.println("NetworkPacket null");
                }


                switch(input.getType()){
                case SERVICE:
                    //System.out.println("ServicePacket with ID: " + input.getOrigin().getUserConnectionId() + "\nSpecifying services: " + ((DummyServiceChain)input.getPath()).toString());
                    if(input.keepAlive()){
                        ServiceRequest s = sendToService(input.getData(), input.getPath());
                        Serializable returnPacket = buildServicePacket(s.getData(), null, getLocalServiceConnection(), PathType.SERVICE);
                        new ObjectOutputStream(clientSocket.getOutputStream()).writeObject(returnPacket);
                    }else{
                        sendToService(input.getData(), input.getPath(), input.getOrigin());
                    }
                    break;
                case DISCOVERY:
                    localServiceDiscovery.discoveryUpdate(input.getOrigin(), serializableToInputStream(input.getData()));
                    break;
                default:
                    System.out.println("Non-servicepacket recieved, not yet implemented: type" + input.getType());

                    break;
                }
                in.close();
                clientSocket.close();
            }catch (Throwable e) {
                System.out.println("Error " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

}
