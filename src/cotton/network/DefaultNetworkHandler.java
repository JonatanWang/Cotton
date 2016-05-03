package cotton.network;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cotton.internalRouting.InternalRoutingNetwork;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Handles all of the packet buffering and relaying.
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public class DefaultNetworkHandler implements NetworkHandler {
    private int localPort;
    private InetAddress localIP;
    private InternalRoutingNetwork internalRouting;
    private ExecutorService threadPool;
    private AtomicBoolean running;
    private SocketAddress localSocketAddress;

    public DefaultNetworkHandler() throws UnknownHostException {
        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
        localSocketAddress = getLocalAddress();
    }

    public DefaultNetworkHandler(SocketAddress socketAddress) throws UnknownHostException {
        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        localSocketAddress = socketAddress;

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
    }

    // This constructor should be replaced by a config file
    public DefaultNetworkHandler(int port) throws UnknownHostException {
        this.localPort = port;
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
        localSocketAddress = getLocalAddress();
    }

    @Override
    public void setInternalRouting(InternalRoutingNetwork internalRouting) {
        if(internalRouting != null)
            this.internalRouting = internalRouting;
        else
            throw new NullPointerException("InternalRoutingNetwork points to null");
    }
    
    private SSLServerSocket createSSLServerSocket() throws IOException {
        SSLServerSocketFactory sf = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        return (SSLServerSocket)sf.createServerSocket();
    }
    
    private SSLSocket createSSLClientSocket(InetSocketAddress address) throws IOException{
        SSLSocketFactory sf = (SSLSocketFactory)SSLSocketFactory.getDefault();
        return (SSLSocket)sf.createSocket(address.getAddress(), address.getPort());
    }

    @Override
    public void run(){
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket();
            serverSocket.bind(getLocalAddress());
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
     * Stops the network-thread.
     */
    @Override
    public void stop(){
        running.set(false);
    }

    /**
     * Warning: improper use of this method can lead to huge network routing bugs,
     *      related to losing the uuid between jumps, use the above method instead
     * @return ServiceConnection with the local socket
     */
    private Origin createOrigin(UUID uuid){
        SocketAddress address = getLocalAddress();
        Origin local = new Origin(address, uuid);
        return local;
    }

    @Override
    public SocketAddress getLocalAddress(){
        SocketAddress address = new InetSocketAddress(localIP, localPort);
        return address;
    }

    private void logError(String error){
        System.out.println("Network exception, " + error); // TODO: MAKE THIS ACTUALLY LOG
    }

    @Override
    public void send(NetworkPacket packet, SocketAddress dest) throws IOException {
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet);
        sendTransportPacket(tp, dest);
    }

    @Override
    public void sendKeepAlive(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet, true);
        sendTransportPacket(tp, dest);
    }

    private void sendTransportPacket(TransportPacket.Packet packet, SocketAddress dest) throws IOException {
        Socket socket = new Socket();
        try {
            //socket = createSSLClientSocket((InetSocketAddress) dest);
            //socket.startHandshake();
            socket.connect(dest);
            packet.writeDelimitedTo(socket.getOutputStream());
        }catch (IOException e) {
            logError("send: " + e.getMessage());
            throw e;
        }finally{
            try {
                socket.close();
            } catch (Throwable e) {
                logError("send socket close: " + e.getMessage());
            }
        }
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
                TransportPacket.Packet input = TransportPacket.Packet.parseDelimitedFrom(clientSocket.getInputStream());

                if(input == null) {
                    System.out.println("TransportPacket null");
                }
                System.out.println("Pathtype is: "+input.getPathtype());

                NetworkPacket np = parseTransportPacket(input);

                if(np.keepAlive()) {
                    SocketLatch latch = new SocketLatch();
                    internalRouting.pushKeepAlivePacket(np, latch);
                    NetworkPacket keepAliveResult = latch.getData();
                    buildTransportPacket(keepAliveResult).writeDelimitedTo(clientSocket.getOutputStream());
                } else {
                    internalRouting.pushNetworkPacket(np);
                }
            } catch(IOException e) {
                e.printStackTrace();
                logError(e.toString());
            } finally {
                try {clientSocket.close();} catch (IOException ex) {}
            }
        }
    }

    private Origin parseOrigin(TransportPacket.Packet input) throws java.net.UnknownHostException{
        TransportPacket.Origin origin = input.getOrigin();
        String ip = origin.getIp();
        int port = origin.getPort();
        String requestId = origin.getRequestId();
        String latchId = origin.getLatchId();
        
        Origin parsedOrigin = new Origin();
        if(ip != "") {
            InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(ip),port);
            parsedOrigin.setAddress(socketAddress);
        }
        if(requestId != "") {
            parsedOrigin.setServiceRequestID(UUID.fromString(requestId));
        }
        if(latchId != "") {
            parsedOrigin.setSocketLatchID(UUID.fromString(latchId));
        }
        
        return parsedOrigin;
    }

    private ServiceChain parsePath(TransportPacket.Packet input){
        DummyServiceChain path = new DummyServiceChain();

        for (int i = 0; i < input.getPathCount(); i++)
            path.addService(input.getPath(i));

        return path;
    }

    private NetworkPacket parseTransportPacket(TransportPacket.Packet input) throws java.net.UnknownHostException{
        ServiceChain path = parsePath(input);
        Origin origin = parseOrigin(input);

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
        UUID serviceRequestID = input.getOrigin().getServiceRequestID();
        UUID socketLatchID = input.getOrigin().getSocketLatchID();
        
        TransportPacket.Origin.Builder originBuilder = TransportPacket.Origin.newBuilder();
        if(address != null) {
            originBuilder = originBuilder
                    .setIp(address.getAddress().getHostAddress())
                    .setPort(address.getPort());
        }
        if(serviceRequestID != null) {
             originBuilder = originBuilder.setRequestId(serviceRequestID.toString());
        }
        if(socketLatchID != null) {
             originBuilder = originBuilder.setLatchId(socketLatchID.toString());
        }
        TransportPacket.Origin origin = originBuilder.build();
        builder.setOrigin(origin);

        builder.setData(com.google.protobuf.ByteString.copyFrom(input.getData()));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(input.getType().toString()));
        builder.setKeepalive(false);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive) throws IOException{
        TransportPacket.Packet.Builder builder = TransportPacket.Packet.newBuilder();

        while (input.getPath().peekNextServiceName() != null) {
            builder.addPath(input.getPath().getNextServiceName());
        }

        InetSocketAddress address = (InetSocketAddress)input.getOrigin().getAddress();
        TransportPacket.Origin origin = TransportPacket.Origin.newBuilder()
            .setIp(address.getAddress().getHostAddress())
            .setRequestId(input.getOrigin().getServiceRequestID().toString())
            .setLatchId(input.getOrigin().getSocketLatchID().toString())
            .setPort(address.getPort())
            .build();
        builder.setOrigin(origin);

        builder.setData(com.google.protobuf.ByteString.copyFrom(input.getData()));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(input.getType().toString()));
        builder.setKeepalive(keepAlive);
        return builder.build();
    }
}
