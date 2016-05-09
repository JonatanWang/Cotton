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
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<SocketAddress, Connection> openSockets;

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
        openSockets = new ConcurrentHashMap<>();
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
        openSockets = new ConcurrentHashMap<>();
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
        openSockets = new ConcurrentHashMap<>();
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
            } catch (SocketTimeoutException ignore) {
            } catch (SocketException e) {
                System.out.println("SocketException " + e.getMessage());
                e.printStackTrace();
                break;
            } catch (Throwable e) {
                System.out.println("Error " + e.getMessage());
                e.printStackTrace();
            }
            timeoutSockets();
        }

        try {
            threadPool.shutdown();
            serverSocket.close();
        }catch (Throwable e) { //TODO EXCEPTION HANDLING
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void timeoutSockets(){
        for(SocketAddress a: openSockets.keySet()){
            Connection c = openSockets.get(a);
            if(c.lastConnectionTime() > c.limit()){
                openSockets.remove(a, c);
                try {
                    c.close();
                }catch (IOException e) {
                    System.out.println("Error " + e.getMessage());
                    e.printStackTrace();
                }
            }
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

    /**
     * Sends data wrapped in a <code>NetworkPacket</code> over the network.
     * It creates a oneway link to the destination, the link is reused every call afterwards.
     * 
     * @param packet contains the data and the <code>metadata</code> needed to send the packet.
     * @param dest defines the <code>SocketAddress</code> to send through.
     * @throws java.io.IOException
     */
    @Override
    public void send(NetworkPacket packet, SocketAddress dest) throws IOException {
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");
        Connection conn = null;
        InetSocketAddress idest = (InetSocketAddress) dest;
        TransportPacket.Packet tp = null;

        for(SocketAddress s: openSockets.keySet()){
            InetSocketAddress is = (InetSocketAddress) s;
            if(is.getAddress().equals(idest.getAddress()) && is.getPort() == idest.getPort()){
               conn = openSockets.get(s);
               tp = buildTransportPacket(packet);
               break;
            }
        }

        if(conn == null){
            Socket socket = new Socket();
            socket.connect(dest);
            conn = new Connection(socket);
            openSockets.putIfAbsent(dest, conn);
            assignListener(conn);
            tp = buildTransportPacket(packet, localPort);
        }

        try {
            tp.writeDelimitedTo(conn.getSocket().getOutputStream());
        }catch(SocketException e){
            openSockets.remove(dest);
            send(packet, dest);
        }catch (IOException e) {
            e.printStackTrace();
            logError("send: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendKeepAlive(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");
        Connection conn = null;
        InetSocketAddress idest = (InetSocketAddress) dest;
        TransportPacket.Packet tp = null;

        for(SocketAddress s: openSockets.keySet()){
            InetSocketAddress is = (InetSocketAddress) s;
            if(is.getAddress().equals(idest.getAddress()) && is.getPort() == idest.getPort()){
                conn = openSockets.get(s);
                tp = buildTransportPacket(packet);
                break;
            }
        }

        if(conn == null){
            Socket socket = new Socket();
            socket.connect(dest);
            conn = new Connection(socket, 50000);
            openSockets.putIfAbsent(dest, conn);
            assignListener(conn);
            tp = buildTransportPacket(packet, true, localPort);
        }

        try {
            tp.writeDelimitedTo(conn.getSocket().getOutputStream());
        }catch(SocketException e){
            openSockets.remove(dest);
            send(packet, dest);
        }catch (IOException e) {
            e.printStackTrace();
            logError("send: " + e.getMessage());
            throw e;
        }
    }

    private void assignListener(Connection s){
        NetworkUnpacker nu = new NetworkUnpacker(s);
        threadPool.execute(nu);
    }

    private class NetworkUnpacker implements Runnable {
        Connection clientConnection = null;

        public NetworkUnpacker(Connection clientConnection){
            this.clientConnection = clientConnection;
        }

        public NetworkUnpacker(Socket socket){
            clientConnection = new Connection(socket);
        }

        @Override
        public void run(){
            Socket clientSocket = clientConnection.getSocket();
            // TODO: ablility  to turn on and off debug msg
            //System.out.println("Incoming connection to: " + clientSocket.getLocalSocketAddress() +" from" + clientSocket.getRemoteSocketAddress());
            try {
                do{
                    clientConnection.updateTime();
                    TransportPacket.Packet input = TransportPacket.Packet.parseDelimitedFrom(clientSocket.getInputStream());

                    if(input == null) {
                        System.out.println("TransportPacket null");
                        return;
                    }

                    if(input.hasLastHopPort()){
                        String address = clientSocket.getInetAddress().getHostAddress();
                        InetSocketAddress s = new InetSocketAddress(address, input.getLastHopPort());
                        openSockets.putIfAbsent(s, clientConnection);
                    }
                    //// TODO: ablility  to turn on and off debug msg
                    //System.out.println("Pathtype is: "+input.getPathtype());

                    NetworkPacket np = parseTransportPacket(input);

                    if(np.keepAlive()) {
                        SocketLatch latch = new SocketLatch();
                        internalRouting.pushKeepAlivePacket(np, latch);
                        NetworkPacket keepAliveResult = latch.getData();
                        buildTransportPacket(keepAliveResult).writeDelimitedTo(clientSocket.getOutputStream());
                    }else
                        internalRouting.pushNetworkPacket(np);
                }while(running.get() == true);
            } catch(com.google.protobuf.InvalidProtocolBufferException e){
                if(!e.getMessage().equals("Socket closed"))
                    e.printStackTrace();// TODO: Logging
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
            .setKeepAlive(input.getKeepalive())
            .build();

        return packet;
    }

    private TransportPacket.Packet.Builder parseNetworkPacket(NetworkPacket input) {
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

        return builder;
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input) throws IOException{
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive) throws IOException{
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, int port) throws IOException{
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        builder.setLastHopPort(port);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive, int port) throws IOException{
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        builder.setLastHopPort(port);
        return builder.build();
    }
}
