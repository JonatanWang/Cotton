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
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


import cotton.configuration.NetworkConfigurator;
import cotton.internalrouting.InternalRoutingNetwork;

import java.nio.channels.ClosedChannelException;
import java.util.Queue;
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
    private ByteBuffer packetSize;
    private InetAddress localIP;
    private InternalRoutingNetwork internalRouting;
    private ExecutorService threadPool;
    private AtomicBoolean running;
    private SocketAddress localSocketAddress;
    private ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels;
    private Queue<SocketChannel> registrationQueue;
    private Selector selector;
    private BlockingQueue<OutputPacket> sendQueue;
    private NetworkOutput sender;
    private boolean encryption;

    /**
     * Returns a <code>SocketSelectionNetworkHandler</code> configured with the options in the <code>NetworkConfigurator</code>.
     *
     * @param config The configuration to follow.
     */
    public SocketSelectionNetworkHandler(NetworkConfigurator config){
        this.localSocketAddress = config.getAddress();
        this.localPort = config.getAddress().getPort();
        this.localIP = config.getAddress().getAddress();
        if(this.encryption = config.isEncryptionEnabled()){
            String keystorePath = config.getKeystore();
            System.setProperty("javax.net.ssl.trustStore", keystorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", config.getPassword());
            System.setProperty("javax.net.ssl.keyStore", keystorePath);
            System.setProperty("javax.net.ssl.keyStorePassword", config.getPassword());
        }

        threadPool = Executors.newFixedThreadPool(20);
        running = new AtomicBoolean(true);
        openChannels = new ConcurrentHashMap<>();

        try {
            selector = Selector.open();
        }catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        threadPool = Executors.newCachedThreadPool();
        running = new AtomicBoolean(true);
        registrationQueue = new ConcurrentLinkedQueue();
        sendQueue = new LinkedBlockingQueue<>();
        sender = new NetworkOutput(this, openChannels, sendQueue);
    }

    /**
     * Returns a <code>SocketSelectionNetworkHandler</code> configured with default options and specified port.
     *
     * @param port The port to use.
     */
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
        sendQueue = new LinkedBlockingQueue<>();
        sender = new NetworkOutput(this, openChannels, sendQueue);
    }

    @Override
    public void setInternalRouting(InternalRoutingNetwork internalRouting) {
        if(internalRouting != null)
            this.internalRouting = internalRouting;
        else
            throw new NullPointerException("InternalRoutingNetwork points to null");
    }

    @Override
    public void run(){
        ServerSocketChannel serverSocketChannel = null;

        try{
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.bind(getLocalAddress());
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch(IOException e){// TODO: Logging
            e.printStackTrace();
        }

        threadPool.execute(sender);

        SocketChannel clientChannel = null;

        while(running.get()){
            if(!registrationQueue.isEmpty()){
                try {
                    registrationQueue.poll().register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException ex) {
                    Logger.getLogger(SocketSelectionNetworkHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try{
                if(selector.select() == 0)
                    continue;
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
                keys.clear();

                // TODO: SocketChannel timeout
                //channelTimeout();

            }catch(IOException e){
                e.printStackTrace();
                //TODO: Exception handling
            }catch(NullPointerException e){
                e.printStackTrace();
            }
        }

        try {
            sender.stop();
            threadPool.shutdown();
            serverSocketChannel.close();
        }catch (Throwable e) { //TODO EXCEPTION HANDLING
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Adds a channel for registration by the network thread.
     *
     * @param s The channel to register.
     */
    protected void registerChannel(SocketChannel s){
        registrationQueue.add(s);
        selector.wakeup();
    }

    /**
     * Stops the network-thread.
     */
    @Override
    public void stop(){
        running.set(false);
    }

    @Override
    public SocketAddress getLocalAddress(){
        return new InetSocketAddress(localIP, localPort);
    }

    private void logError(String error){
        System.out.println("Network exception, " + error); // TODO: MAKE THIS ACTUALLY LOG
    }

    @Override
    public void send(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        this.sendQueue.add(new OutputPacket(packet, dest, false));
    }

    @Override
    public void sendKeepAlive(NetworkPacket packet, SocketAddress dest) throws IOException{
        if(packet == null) throw new NullPointerException("Null data");
        if(dest == null) throw new NullPointerException("Null destination");

        this.sendQueue.add(new OutputPacket(packet, dest, true));
    }

    private InputStream readInput(SocketChannel sc){
        if(packetSize==null)
            packetSize = ByteBuffer.allocate(4);
        try {
            while(packetSize.hasRemaining())
                sc.read(packetSize);

            int size = packetSize.getInt(0);
            packetSize.clear();

            ByteBuffer packet = ByteBuffer.allocate(size);

            while(packet.hasRemaining())
                sc.read(packet);

            packet.flip();

            return new ByteArrayInputStream(packet.array());
        } catch(IOException e) {
            System.out.println("Read input error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private NetworkPacket getInput(SocketChannel sc){
        if(packetSize == null)
            packetSize = ByteBuffer.allocate(4);

        try{
            while(packetSize.hasRemaining())
                sc.read(packetSize);

            int size = packetSize.getInt(0);
            packetSize.clear();

            while(packetSize.hasRemaining())
                sc.read(packetSize);

            PathType type = PathType.values()[packetSize.getInt(0)];
            packetSize.clear();

            ByteBuffer packet = ByteBuffer.allocate(size);

            while(packet.hasRemaining())
                sc.read(packet);

            packet.flip();
            return new NetworkPacket(type, packet);
        } catch(IOException e) {
            System.out.println("Read input error: " + e.toString());
            e.printStackTrace();
            return null;
        }

    }

    private void handleRequest(SocketChannel channel) throws IOException{
        //InputStream inStream = readInput(channel);
        NetworkPacket packet = getInput(channel);

        //if(inStream == null){
        if(packet == null){
            return;
        }

        //TransportPacket.Packet input = TransportPacket.Packet.parseDelimitedFrom(inStream);

        //if(input == null) {
        //    System.out.println("TransportPacket null");
        //    return;
        //}

        //NetworkPacket np = parseTransportPacket(input);

        //if(input.getSerializedSize() == 0) {
        //    System.out.println("Empty transportpacket");
        //    return;
        //}

        //System.out.println(np.getType()+" packet of "+input.getSerializedSize()+" bytes received on "+getLocalAddress()+".");

        internalRouting.pushNetworkPacket(packet);
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

}
