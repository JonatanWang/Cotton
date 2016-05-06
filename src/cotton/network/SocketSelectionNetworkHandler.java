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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import cotton.internalRouting.InternalRoutingNetwork;
import static cotton.network.PathType.REQUESTQUEUE;
import java.nio.channels.ClosedChannelException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all of the packet buffering and relaying.
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
public class SocketSelectionNetworkHandler implements NetworkHandler {
    private int localPort;
    private InetAddress localIP;
    private InternalRoutingNetwork internalRouting;
    private ExecutorService threadPool;
    private AtomicBoolean running;
    private SocketAddress localSocketAddress;
    private ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels;
    private Queue<SocketChannel> registrationQueue;
    private Selector selector;

    // TODO: All constructors should initiate a SSLEngine for use with non-blocking net IO
    public SocketSelectionNetworkHandler() throws UnknownHostException {
        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }
        try {
            selector = Selector.open();
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
        localSocketAddress = getLocalAddress();
        openChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
        registrationQueue = new ConcurrentLinkedQueue();
    }

    public SocketSelectionNetworkHandler(SocketAddress socketAddress) throws UnknownHostException {
        this.localPort = 3333; // TODO: Remove hardcoded port
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }
        try {
            selector = Selector.open();
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        localSocketAddress = socketAddress;

        threadPool = Executors.newCachedThreadPool();
        openChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
        running = new AtomicBoolean(true);
        registrationQueue = new ConcurrentLinkedQueue();
    }

    // This constructor should be replaced by a config file
    public SocketSelectionNetworkHandler(int port) throws UnknownHostException {
        this.localPort = port;
        try{
            //this.localIP = InetAddress.getByName(null);
            this.localIP = Inet4Address.getLocalHost();
        }catch(java.net.UnknownHostException e){// TODO: Get address from outside
            logError("initialization process local address "+e.getMessage());
            throw e;
        }
        try {
            selector = Selector.open();
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
        localSocketAddress = getLocalAddress();
        openChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
        registrationQueue = new ConcurrentLinkedQueue();
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
        //ServerSocket serverSocket = null;
        ServerSocketChannel serverSocketChannel = null;
        //Selector selector = null;

        try{
            //serverSocket = new ServerSocket();
            //serverSocket.bind(getLocalAddress());
            //serverSocket.setSoTimeout(80);
            serverSocketChannel = ServerSocketChannel.open();
            //selector = Selector.open();

            serverSocketChannel.bind(getLocalAddress());
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, serverSocketChannel.validOps());
        }catch(IOException e){// TODO: Logging
            e.printStackTrace();
        }

        SocketChannel clientChannel = null;

        while(running.get() == true){
            if(!registrationQueue.isEmpty()){
                try {
                    registrationQueue.poll().register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException ex) {
                    Logger.getLogger(SocketSelectionNetworkHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try{
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();

                for(SelectionKey key: keys) {
                    if(key.isAcceptable()){
                        clientChannel = serverSocketChannel.accept();
                        if(clientChannel != null){
                            clientChannel.configureBlocking(false);
                            openChannels.putIfAbsent((InetSocketAddress)clientChannel.getRemoteAddress(), clientChannel);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                        }
                    } else if(key.isReadable()) {
                        clientChannel = (SocketChannel) key.channel();
                        handleRequest(clientChannel);
                    }
                    clientChannel = null;
                }

                // TODO: SocketChannel timeout
                //channelTimeout();

            }catch(IOException e){
                e.printStackTrace();
                //TODO: Exception handling
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            /*
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
                }*/
        }

        try {
            threadPool.shutdown();
            serverSocketChannel.close();
        }catch (Throwable e) { //TODO EXCEPTION HANDLING
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

    }
    
    private void registerChannel(SocketChannel s){
        registrationQueue.add(s);
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

    private ConcurrentHashMap<InetSocketAddress, Socket> liveSockets = new ConcurrentHashMap<InetSocketAddress, Socket>();
    
    private boolean storeSocket(SocketAddress dest,Socket socket) {
        return liveSockets.putIfAbsent((InetSocketAddress)dest, socket) == null;
    }
    
    private Socket removeSocket(SocketAddress dest) {
        return liveSockets.remove((InetSocketAddress)dest);
    }
    
    private Socket reuseSocket(SocketAddress dest) {
        return liveSockets.get((InetSocketAddress)dest);
    }
    
    private void fallbackSend(TransportPacket.Packet tp, SocketAddress dest) throws IOException {
        Socket socket = null;
        boolean keepSocket = false;
        try {
            socket = new Socket();
            socket.connect(dest);
            keepSocket = storeSocket(dest, socket);
            tp.writeDelimitedTo(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            logError("fallbackSend: " + e.getMessage());
            throw e;
        }finally {
            try {
                if(!keepSocket && socket != null) {
                    socket.close();
                }
            } catch (Throwable e) {
                logError("fallbackSend socket close: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sends data wrapped in a <code>NetworkPacket</code> over the network.
     * It creates a oneway link to the destination, the link is reused every call afterwards.
     * 
     * @param netPacket contains the data and the <code>metadata</code> needed to send the packet.
     * @param dest defines the <code>SocketAddress</code> to send through.
     * @throws java.io.IOException
     */
    /*public void sendOverActiveLink(NetworkPacket packet, SocketAddress dest) throws IOException {
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet);
        Socket socket = reuseSocket(dest);

        boolean keepSocket = true;
        try {
            if(socket == null) {
                socket = new Socket();
                socket.connect(dest);
                keepSocket = storeSocket(dest,socket);
            }
            tp.writeDelimitedTo(socket.getOutputStream());
        }catch(SocketException e){
            Socket removeSocket = removeSocket(dest);
            if(removeSocket != null)
                removeSocket.close();
            socket.close();
            fallbackSend(tp,dest);
        }catch (IOException e) {
            e.printStackTrace();
            logError("send: " + e.getMessage());
            throw e;
        }finally{
            try {
                if(!keepSocket) {
                    socket.close();
                }
            } catch (Throwable e) {
                logError("send socket close: " + e.getMessage());
            }
        }
        }*/

    @Override
    public synchronized void sendOverActiveLink(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet);

        ByteBuffer output = writeOutput(tp);

        SocketChannel sendChannel = null;

        System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" active link send.");

        if((sendChannel = openChannels.get((InetSocketAddress) dest)) != null){
            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity()); 
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
        } else {
            sendChannel = SocketChannel.open();
            sendChannel.connect(dest);
            sendChannel.configureBlocking(false);

            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity()); 
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
            registerChannel(sendChannel);
            selector.wakeup();
            openChannels.putIfAbsent((InetSocketAddress)dest, sendChannel);
        }
    }

    @Override
    public void send(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet);

        ByteBuffer output = writeOutput(tp);

        SocketChannel sendChannel = null;

        System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" normal send.");

        if((sendChannel = openChannels.get((InetSocketAddress) dest)) != null){
            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity()); 
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
        } else {
            sendChannel = SocketChannel.open();
            sendChannel.connect(dest);
            sendChannel.configureBlocking(false);

            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity()); 
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
            registerChannel(sendChannel);
            selector.wakeup();
            openChannels.putIfAbsent((InetSocketAddress)dest, sendChannel);
        }
    }

    /*@Override
    public void send(NetworkPacket packet, SocketAddress dest) throws IOException {
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        TransportPacket.Packet tp = buildTransportPacket(packet);

        Socket socket = new Socket();
        try {
            socket.connect(dest);
            tp.writeDelimitedTo(socket.getOutputStream());
        }catch (IOException e) {
            e.printStackTrace();
            logError("send: " + e.getMessage());
            throw e;
        }finally{
            try {
                socket.close();
            } catch (Throwable e) {
                logError("send socket close: " + e.getMessage());
            }
        }
    }*/

    @Override
    public void sendKeepAlive(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        System.out.println("Keepalive send!");

        TransportPacket.Packet tp = buildTransportPacket(packet, true);

        ByteBuffer output = writeOutput(tp);
        
        SocketChannel sendChannel = null;

        System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" keepalive send.");

        if((sendChannel = openChannels.get((InetSocketAddress) dest)) != null){
            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
        } else {
            sendChannel = SocketChannel.open();
            sendChannel.connect(dest);
            sendChannel.configureBlocking(false);

            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
            sendChannel.write(size);
            // String s = "";
            // for(byte b: size.array())
            //     s = s+" "+b;
            // System.out.println(s);

            sendChannel.write(output);
            registerChannel(sendChannel);
            selector.wakeup();
            openChannels.putIfAbsent((InetSocketAddress)dest, sendChannel);
        }

        /*TransportPacket.Packet tp = buildTransportPacket(packet, true);

        Socket socket = new Socket();
        try {
            socket.connect(dest);
            tp.writeDelimitedTo(socket.getOutputStream());
            //NetworkUnpacker nu = new NetworkUnpacker(socket);

            //threadPool.execute(nu);
        }catch (IOException e) {
            logError("send: " + e.getMessage());
            throw e;
            }*/
    }

    private InputStream readInput(SocketChannel sc){
        ByteBuffer bb = ByteBuffer.allocate(4);

        try {
            sc.read(bb);
            /*String s = "";
                for(byte b: bb.array())
                    s = s+" "+b;
                    System.out.println(s);*/

            int size = bb.getInt(0);
            if(size <= 0){
                //openChannels.remove(sc);
                //sc.close();
                return null;
            }
            System.out.println("Incoming bytebuffer, "+size+" bytes on "+getLocalAddress()+".");

            bb = ByteBuffer.allocate(size);

            size = sc.read(bb);

            bb.flip();

            return new ByteArrayInputStream(bb.array());
        } catch(IOException e) {
            System.out.println("Read input error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuffer writeOutput(TransportPacket.Packet tp) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        tp.writeDelimitedTo(baos);
        ByteBuffer b = ByteBuffer.wrap(baos.toByteArray());

        return b;
    }

    /*private class NetworkUnpacker implements Runnable {
        Socket clientSocket = null;
        public NetworkUnpacker(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            // TODO: Turn debugging messages on or off
            //System.out.println("Incoming connection to: " + clientSocket.getLocalSocketAddress() +" from" + clientSocket.getRemoteSocketAddress());
            try {
            while(running.get == true){*/
    private void handleRequest(SocketChannel channel) throws IOException{
        InputStream inStream = readInput(channel);

        if(inStream == null){
            return;
        }

        TransportPacket.Packet input = TransportPacket.Packet.parseDelimitedFrom(inStream);

        if(input == null) {
            System.out.println("TransportPacket null");
            return;
        }

        NetworkPacket np = parseTransportPacket(input);

        if(input.getSerializedSize() == 0)
            return;

        System.out.println(np.getType()+" packet of "+input.getSerializedSize()+" bytes received on "+getLocalAddress()+".");

        if(np.keepAlive()) {
            SocketLatch latch = new SocketLatch();
            internalRouting.pushKeepAlivePacket(np, latch);
            NetworkPacket keepAliveResult = latch.getData();

            TransportPacket.Packet outputPacket = buildTransportPacket(keepAliveResult);
            ByteBuffer output = writeOutput(outputPacket);

            System.out.println("Keepalive received, sending result "+output.capacity()+" bytes.");

            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
            channel.write(size);
            channel.write(output);
        } else {
            internalRouting.pushNetworkPacket(np);
        }
    }
    /*
      } catch(IOException e) {
      e.printStackTrace();
      logError(e.toString());
      } finally {
      try {clientSocket.close();} catch (IOException ex) {}
      }
      }
      }*/

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

}
