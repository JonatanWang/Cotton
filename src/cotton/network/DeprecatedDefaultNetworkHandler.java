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
import cotton.services.DeprecatedServicePacket;
import cotton.servicediscovery.DeprecatedServiceDiscovery;
import cotton.network.DeprecatedNetworkPacket;
import cotton.services.DeprecatedServiceBuffer;

/**
 * Handles all of the packet buffering and relaying.
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
@Deprecated
public class DeprecatedDefaultNetworkHandler implements DeprecatedNetworkHandler,ClientNetwork {
    private DeprecatedServiceBuffer serviceBuffer;
    private ConcurrentHashMap<UUID,DeprecatedDefaultServiceRequest> connectionTable;
    private AtomicBoolean running;
    private DeprecatedServiceDiscovery localServiceDiscovery;
    private int localPort;
    private InetAddress localIP;
    private ExecutorService threadPool;
    private SocketAddress localSocketAddress;

    public DeprecatedDefaultNetworkHandler() throws java.net.UnknownHostException{
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
    }

    public DeprecatedDefaultNetworkHandler(DeprecatedServiceDiscovery localServiceDiscovery) throws java.net.UnknownHostException{
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

    public DeprecatedDefaultNetworkHandler(DeprecatedServiceDiscovery localServiceDiscovery,SocketAddress localSocketAddress) throws java.net.UnknownHostException{
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

    public DeprecatedDefaultNetworkHandler(DeprecatedServiceDiscovery localServiceDiscovery,int port) throws java.net.UnknownHostException{
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

    public boolean sendTransportPacket(TransportPacket.Packet data, ServiceConnection destination) throws IOException{
        if(data == null) throw new NullPointerException("Null data");
        if(destination == null) throw new NullPointerException("Null destination");

        Socket socket = new Socket(); // TODO: Encryption
        try {
            socket.connect(destination.getAddress());
            data.writeTo(socket.getOutputStream());
            //new ObjectOutputStream(socket.getOutputStream()).writeObject(data);
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
    private boolean sendObject(String data, ServiceConnection destination) {
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
    public boolean send(byte[] data, ServiceConnection destination) throws IOException {
        TransportPacket.Packet packet = buildTransportPacket(data,
                                                             null,
                                                             getLocalServiceConnection(destination.getUserConnectionId()),
                                                             destination.getPathType());

        try{
            return sendTransportPacket(packet, destination);
        }catch(IOException e){
            logError("Failed to send packet");
            throw e;
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
    public boolean send(Serializable data, ServiceConnection destination) throws IOException {
        TransportPacket.Packet packet = buildTransportPacket(serializableToBytes(data),
                                                             null,
                                                             getLocalServiceConnection(destination.getUserConnectionId()),
                                                             destination.getPathType());

        try{
            return sendTransportPacket(packet, destination);
        }catch(IOException e){
            logError("Failed to send packet");
            throw e;
        }
    }

    /**
     * Sends a serializable object to the specified destination, returning a response.
     *
     * @param data The object to send.
     * @param destination The information about where the packet is headed.
     * @return The result with which the response will be returned.
     */
    @Override
    public DeprecatedServiceRequest sendWithResponse(Serializable data, ServiceConnection destination) throws IOException{
        DeprecatedDefaultServiceRequest result = new DeprecatedDefaultServiceRequest();
        ServiceConnection local = getLocalServiceConnection();
        TransportPacket.Packet packet = buildTransportPacket(serializableToBytes(data), null, local, destination.getPathType());
        if(sendTransportPacket(packet, destination)){
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
    public void sendToService(byte[] data, ServiceChain path, ServiceConnection from) throws IOException {
        ServiceConnection dest = new DefaultServiceConnection();
        RouteSignal route = localServiceDiscovery.getDestination(dest, from, path);

        switch(route){
        case LOCALDESTINATION:
            sendToServiceBuffer(from, data, path);
            return;
        case ENDPOINT:
            DeprecatedDefaultServiceRequest req = connectionTable.get(from.getUserConnectionId());
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

        TransportPacket.Packet packet = buildTransportPacket(data, path, from, dest.getPathType());

        sendTransportPacket(packet, dest);
    }

    /**
     * Sends a request to a service, destination is decided by the servicediscovery with this node as origin.
     *
     * @param data The object to send.
     * @param path The predefined services to pass through.
     */
    @Override
    public DeprecatedServiceRequest sendToService(byte[] data, ServiceChain path) throws IOException { // TODO: Make sure that this doesn't drop packages with this as last destination
        UUID uuid = UUID.randomUUID();
        ServiceConnection dest = new DefaultServiceConnection(uuid);
        RouteSignal route = localServiceDiscovery.getDestination(dest, path);
        DeprecatedDefaultServiceRequest result = new DeprecatedDefaultServiceRequest();
        this.connectionTable.put(dest.getUserConnectionId(), result);
        if(route == RouteSignal.LOCALDESTINATION) {
            sendToServiceBuffer(dest, data, path);
            return result;
        }
        if(route == RouteSignal.RETURNTOORIGIN) {
            // TODO: implement this
        }

        TransportPacket.Packet packet = buildTransportPacket(data, path, getLocalServiceConnection(dest.getUserConnectionId()), dest.getPathType());

        try{
            if(sendTransportPacket(packet, dest)){
                return result;
            }
            return null;
        }catch(IOException e){
            logError("Failed to send packet");
            throw e;
        }
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
    public DeprecatedServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    /**
     * Stops the network-thread.
     */
    public void stop(){
        running.set(false);
    }

    /**
     * Sends the data to a local service waiting on the data , puts it in the right ServiceRequest,
     * @param data the result of a client ServiceRequest
     * @param destination
     * @return
     */
    public boolean sendEnd(byte[] data, ServiceConnection destination) {
        DeprecatedDefaultServiceRequest req = this.connectionTable.get(destination.getUserConnectionId());
        if(req == null) return false;
        req.setData(data);
        return true;
    }

    private byte[] serializableToBytes(Serializable data) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    private void sendToServiceBuffer(ServiceConnection from, byte[] data, ServiceChain path) {
        DeprecatedServicePacket servicePacket = new DeprecatedServicePacket(from, data, path);
        serviceBuffer.add(servicePacket);
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
                //ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                TransportPacket.Packet input = TransportPacket.Packet.parseDelimitedFrom(clientSocket.getInputStream());

                //System.out.println("Socket closed, parsing packet!");
                if(input == null) {
                    System.out.println("TransportPacket null");
                }
                System.out.println("Pathtype is: "+input.getPathtype());


                switch(input.getPathtype()){
                case SERVICE:
                    /*System.out.println("ServicePacket with ID: "
                                       + input.getOrigin().getUuid()
                      + "\nSpecifying services: "
                      + ((DummyServiceChain)input.getPath()).toString());*/
                  
                    if(input.getKeepalive()){
                        System.out.println("Keepalive");
                        System.out.println("Services: "+parsePath(input).toString());
                        DeprecatedServiceRequest s = sendToService(input.getData().toByteArray(),
                                                                   parsePath(input));

                        TransportPacket.Packet returnPacket = buildTransportPacket(s.getData(),
                                                                                   new DummyServiceChain(),
                                                                                   getLocalServiceConnection(),
                                                                                   PathType.SERVICE);

                        returnPacket.writeTo(clientSocket.getOutputStream());
                    }else{
                        sendToService(input.getData().toByteArray(), parsePath(input), parseOrigin(input));
                    }
                    break;
                case DISCOVERY:
                    ServiceConnection origin = parseOrigin(input);
                    localServiceDiscovery.discoveryUpdate(origin, input.getData().toByteArray());
                    break;
                default:
                    System.out.println("Non-servicepacket recieved, not yet implemented: type" + input.getPathtype());

                    break;
                }
                //in.close();
                clientSocket.close();
            }catch (Throwable e) {
                System.out.println("Error " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private ServiceConnection parseOrigin(TransportPacket.Packet input) throws java.net.UnknownHostException{
        ServiceConnection origin = new DefaultServiceConnection(UUID.fromString(input.getOrigin().getRequestId()));
        origin.setAddress(new InetSocketAddress(Inet4Address.getByName(input.getOrigin().getIp()), input.getOrigin().getPort()));
        return origin;
    }

    private ServiceChain parsePath(TransportPacket.Packet input){
        DummyServiceChain path = new DummyServiceChain();
        for (int i = 0; i < input.getPathCount(); i++)
            path.addService(input.getPath(i));
        return path;
    }

    private NetworkPacket parseTransportPacket(TransportPacket.Packet input) throws java.net.UnknownHostException{
        DummyServiceChain path = new DummyServiceChain();
        for (int i = 0; i < input.getPathCount(); i++)
            path.addService(input.getPath(i));

        Origin origin = new Origin(new InetSocketAddress(Inet4Address.getByName(input.getOrigin().getIp()), input.getOrigin().getPort()), UUID.fromString(input.getOrigin().getRequestId()));

        NetworkPacket packet = NetworkPacket.newBuilder()
            .setData(input.getData().toByteArray())
            .setPath(path)
            .setOrigin(origin)
            .setPathType(PathType.valueOf(input.getPathtype().toString()))
            .build();

        return packet;
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input) throws IOException{
        TransportPacket.Packet.Builder builder = TransportPacket.Packet.newBuilder();

        while (input.getPath().peekNextServiceName() != null) {
            builder.addPath(input.getPath().getNextServiceName());
        }

        InetSocketAddress address = (InetSocketAddress)input.getOrigin().getAddress();
        TransportPacket.Origin origin = TransportPacket.Origin.newBuilder()
            .setIp(address.getAddress().getHostAddress())
            .setRequestId(input.getOrigin().getServiceRequestID().toString())
            .setPort(address.getPort())
            .build();
        builder.setOrigin(origin);

        builder.setData(com.google.protobuf.ByteString.copyFrom(input.getData()));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(input.getType().toString()));
        builder.setKeepalive(false);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(byte[] data, ServiceChain path, ServiceConnection origin, PathType type){
        TransportPacket.Packet.Builder builder = TransportPacket.Packet.newBuilder();

        while (path.peekNextServiceName() != null) {
            builder.addPath(path.getNextServiceName());
        }

        InetSocketAddress address = (InetSocketAddress)origin.getAddress();
        TransportPacket.Origin originInfo = TransportPacket.Origin.newBuilder()
            .setIp(address.getAddress().getHostAddress())
            .setRequestId(origin.getUserConnectionId().toString())
            .setPort(address.getPort())
            .build();
        builder.setOrigin(originInfo);

        builder.setData(com.google.protobuf.ByteString.copyFrom(data));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(type.toString()));
        builder.setKeepalive(false);
        return builder.build();
    }

}
