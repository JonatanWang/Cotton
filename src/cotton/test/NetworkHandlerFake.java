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
package cotton.test;

import cotton.network.*;
import com.google.protobuf.InvalidProtocolBufferException;
import cotton.internalrouting.InternalRoutingNetwork;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class NetworkHandlerFake implements NetworkHandler {

    private InternalRoutingNetwork internalRouting = null;
    private InetSocketAddress localAddress = null;
    private ConcurrentHashMap<SocketAddress, Socket> openSocket;
    private ExecutorService threadPool;

    public NetworkHandlerFake(int portNumber) throws UnknownHostException {
        this.localAddress = new InetSocketAddress(Inet4Address.getLocalHost(), portNumber);
        this.openSocket = new ConcurrentHashMap<SocketAddress, Socket>();
        this.threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(100);
    }

    private Socket getConnection(SocketAddress dest) {
        return openSocket.get(dest);
    }

    private boolean addConnection(SocketAddress dest, Socket socket) {
        //System.out.println("addConnection openSocketCount: " + this.openSocket.size());
        Socket put = this.openSocket.put(dest, socket);
        if (put != null) {
            return false;
        }
        return true;
    }

    private boolean removeConnection(SocketAddress dest, Socket socket) {
        return this.openSocket.remove(dest, socket);
    }

    private AtomicInteger createCount = new AtomicInteger(0);

    private Socket createConnection(SocketAddress dest) throws IOException {
        Socket socket = new Socket();
        socket.setKeepAlive(true);
        socket.setSoLinger(true, 0);
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        socket.connect(dest);
        //socket.setTcpNoDelay(true);
        int count = createCount.incrementAndGet();
        System.out.println("createConnection count: " + count 
                + " ip: " + localAddress.getAddress().getHostAddress() +" port:" + localAddress.getPort());
        Socket ret = this.openSocket.putIfAbsent(dest, socket);
        if (ret != null) {
            if (ret.isConnected()) {
                socket.close();
                return ret;
            }
            this.openSocket.remove(dest, ret);
            ret.close();
            ret = this.openSocket.putIfAbsent(dest, socket);
            return socket;
        }
        return socket;
    }

    @Override
    public void send(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        Socket connection = getConnection(dest);
        //System.out.println("OpenSockets: " + this.openSocket.size());
        if (connection != null) {
            TransportPacket.Packet pkt = buildTransportPacket(netPacket);
            try {
                pkt.writeDelimitedTo(connection.getOutputStream());
                return;
            } catch (SocketException ex) {
                removeConnection(dest, connection);
                //Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                removeConnection(dest, connection);
                //Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        connection = createConnection(dest);
        TransportPacket.Packet pkt = this.buildTransportPacket(netPacket, this.localAddress.getPort());//buildTransportPacket(netPacket);
        try {
            pkt.writeDelimitedTo(connection.getOutputStream());
            NetworkDataGetter nget = new NetworkDataGetter(connection, dest);
            this.threadPool.execute(nget);
            return;
        } catch (IOException ex) {
            removeConnection(dest, connection);
            Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void sendKeepAlive(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        Socket connection = createConnection(dest);
        if (connection != null) {
            TransportPacket.Packet pkt = buildTransportPacket(netPacket, true);
            try {
                pkt.writeDelimitedTo(connection.getOutputStream());

            } catch (SocketException ex) {
                removeConnection(dest, connection);
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (IOException ex) {
                removeConnection(dest, connection);
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        this.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                TransportPacket.Packet incoming = null;
                try {
                    boolean flag = false;
                    do {
                        incoming = proccessIncoming(connection);
                        if (incoming != null) {
                            NetworkPacket np = parseTransportPacket(incoming);
                            internalRouting.pushNetworkPacket(np);
                            flag = true;
                        }
                    } while (!flag);
                } catch (InvalidProtocolBufferException ex) {
                    System.out.println("NetworkDataGetter run: InvalidProtocolBufferException null"); //proccessIncoming
                    ex.printStackTrace();
                } catch (IOException ex) {
                    Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    removeConnection(dest, connection);
                    connection.close();
                } catch (IOException ex) {
                    Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public void setInternalRouting(InternalRoutingNetwork internal) {
        this.internalRouting = internal;
    }

    @Override
    public void stop() {
        this.threadPool.shutdownNow();
        try {
            if (serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private ServerSocket serverSocket = null;

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(this.localAddress);
            serverSocket.setSoTimeout(40);
            //serverSocket.setReuseAddress(true);
        } catch (IOException ex) {
            Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
        }

        Socket client = null;
        do {
            try {
                if ((client = serverSocket.accept()) != null) {
                    //client.setTcpNoDelay(true);
                    client.setKeepAlive(true);
                    client.setSoLinger(true, 0);
                    client.setReuseAddress(true);
                    client.setTcpNoDelay(true);
                    NetworkDataGetter nget = new NetworkDataGetter(client);
                    this.threadPool.execute(nget);
                }
            } catch (SocketTimeoutException ignore) {
            } catch (SocketException e) {
                System.out.println("SocketException " + e.getMessage());
                e.printStackTrace();
                break;
            } catch (IOException ex) {
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (true);
        try {
            serverSocket.close();

        } catch (IOException ex) {
            Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<SocketAddress, Socket> entry : this.openSocket.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException ex) {
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Origin parseOrigin(TransportPacket.Packet input) throws java.net.UnknownHostException {
        TransportPacket.Origin origin = input.getOrigin();
        String ip = origin.getIp();
        int port = origin.getPort();
        String requestId = origin.getRequestId();
        String latchId = origin.getLatchId();

        Origin parsedOrigin = new Origin();
        if (ip != "") {
            InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(ip), port);
            parsedOrigin.setAddress(socketAddress);
        }
        if (requestId != "") {
            parsedOrigin.setServiceRequestID(UUID.fromString(requestId));
        }
        if (latchId != "") {
            parsedOrigin.setSocketLatchID(UUID.fromString(latchId));
        }

        return parsedOrigin;
    }

    private ServiceChain parsePath(TransportPacket.Packet input) {
        DummyServiceChain path = new DummyServiceChain();

        if (input == null) {
            return path;
        }
        for (int i = 0; i < input.getPathCount(); i++) {
            path.addService(input.getPath(i));
        }

        return path;
    }

    private NetworkPacket parseTransportPacket(TransportPacket.Packet input) throws java.net.UnknownHostException {
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

        while (input.getPath() != null && input.getPath().peekNextServiceName() != null) {
            builder.addPath(input.getPath().getNextServiceName());
        }

        InetSocketAddress address = (InetSocketAddress) input.getOrigin().getAddress();
        UUID serviceRequestID = input.getOrigin().getServiceRequestID();
        UUID socketLatchID = input.getOrigin().getSocketLatchID();

        TransportPacket.Origin.Builder originBuilder = TransportPacket.Origin.newBuilder();
        if (address != null) {
            originBuilder = originBuilder
                    .setIp(address.getAddress().getHostAddress())
                    .setPort(address.getPort());
        }
        if (serviceRequestID != null) {
            originBuilder = originBuilder.setRequestId(serviceRequestID.toString());
        }
        if (socketLatchID != null) {
            originBuilder = originBuilder.setLatchId(socketLatchID.toString());
        }
        TransportPacket.Origin origin = originBuilder.build();
        builder.setOrigin(origin);

        builder.setData(com.google.protobuf.ByteString.copyFrom(input.getData()));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(input.getType().toString()));

        return builder;
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, int port) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        builder.setLastHopPort(port);
        return builder.build();
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive, int port) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        builder.setLastHopPort(port);
        return builder.build();
    }

    private TransportPacket.Packet proccessIncoming(Socket connection) throws IOException {
        if (connection.isInputShutdown()) {
            System.out.println("TransportPacket the input thing");
            return null;
        }
        if (connection.isOutputShutdown()) {
            System.out.println("TransportPacket the output thing");
            return null;
        }
        if (connection.isClosed()) {
            System.out.println("TransportPacket the close thing");
            return null;
        }
        TransportPacket.Packet input = null;
        input = TransportPacket.Packet.parseDelimitedFrom(connection.getInputStream());

        if (input == null) {
            System.out.println("TransportPacket null");
            return null;
        }
        return input;
    }

    private class NetworkDataGetter implements Runnable {

        private Socket connection;
        private SocketAddress connectedDestination = null;
        private boolean start = false;
        private TransportPacket.Packet startPacket = null;
        private boolean shutdown = false;

        public NetworkDataGetter(Socket connection, SocketAddress connectedDestination) {
            this.connection = connection;
            this.connectedDestination = connectedDestination;
            this.start = true;
        }

        private void specialStart() {
            try {

                this.startPacket = proccessIncoming(this.connection);
                if (startPacket.hasLastHopPort()) {
                    int lastHopPort = startPacket.getLastHopPort();
                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) connection.getRemoteSocketAddress();
                    if (remoteSocketAddress == null) {
                        throw new IOException("RemoteSocket closed");
                    }
                    SocketAddress remoteDest = new InetSocketAddress(remoteSocketAddress.getAddress(), lastHopPort);
                    this.start = addConnection(remoteDest, connection);;

                }
            } catch (IOException ex) {
                this.shutdown = true;
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public NetworkDataGetter(Socket connection) {
            this.connection = connection;
        }

        private void killConnection() {
            try {
                if (connectedDestination != null) {
                    removeConnection(connectedDestination, connection);
                }
                connection.close();
            } catch (IOException ex) {
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            if (this.connectedDestination == null) {
                specialStart();
            }
            if (this.shutdown) {
                return;
            }
            try {
                if (startPacket != null) {
                    NetworkPacket np = parseTransportPacket(startPacket);
                    if (np.keepAlive()) {
                        SocketLatch latch = new SocketLatch();
                        internalRouting.pushKeepAlivePacket(np, latch);
                        NetworkPacket data = latch.getData();
                        buildTransportPacket(data).writeDelimitedTo(connection.getOutputStream());
                        killConnection();
                        return;

                    } else {
                        internalRouting.pushNetworkPacket(np);
                    }

                }
                TransportPacket.Packet incoming = null;
                int nullThreshold = 0;
                do {
                    incoming = null;
                    try {
                        incoming = proccessIncoming(this.connection);
                    } catch (InvalidProtocolBufferException ex) {
                        //System.out.println("NetworkDataGetter run: InvalidProtocolBufferException null:" 
                        //+ localAddress.getAddress().getHostAddress() +" port:" + localAddress.getPort()); 
                        //ex.printStackTrace();
                    }
                    if (incoming != null) {
                        NetworkPacket np = parseTransportPacket(incoming);
                        internalRouting.pushNetworkPacket(np);
                    } else {
                        nullThreshold++;
                    }
                } while (nullThreshold > 1);
            } catch (InvalidProtocolBufferException ex) {
                System.out.println("NetworkDataGetter run: InvalidProtocolBufferException null" 
                        + localAddress.getAddress().getHostAddress() +" port:" + localAddress.getPort()); //proccessIncoming
                ex.printStackTrace();
            } catch (IOException ex) {
                Logger.getLogger(NetworkHandlerFake.class.getName()).log(Level.SEVERE, null, ex);
            }
            killConnection();

        }

    }
}
