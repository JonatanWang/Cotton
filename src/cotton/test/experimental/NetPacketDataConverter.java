/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test.experimental;

import cotton.internalrouting.InternalRoutingNetwork;
import cotton.network.DefaultServiceChain;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.TransportPacket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 *
 * @author Magnus
 * @author Jonathan
 * @author Gunnlaugur
 */
public class NetPacketDataConverter {
    private InternalRoutingNetwork routing;

    public NetPacketDataConverter(InternalRoutingNetwork routing) {
        this.routing = routing;
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
        DefaultServiceChain path = new DefaultServiceChain();

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
    
    private static TransportPacket.Packet.Builder parseNetworkPacket(NetworkPacket input) {
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

    public static TransportPacket.Packet buildTransportPacket(NetworkPacket input) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        return builder.build();
    }

    public static TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        return builder.build();
    }

    public static TransportPacket.Packet buildTransportPacket(NetworkPacket input, int port) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(false);
        builder.setLastHopPort(port);
        return builder.build();
    }

    public static TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive, int port) throws IOException {
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        builder.setLastHopPort(port);
        return builder.build();
    }

    public static ByteBuffer writeOutput(TransportPacket.Packet tp) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        tp.writeDelimitedTo(baos);
        byte[] tpByte = baos.toByteArray();
        int size = tpByte.length;
        final int  intSize = 4;
        ByteBuffer raw = ByteBuffer.allocate(size + intSize);
        raw.putInt(size);
        raw.put(tpByte, 0, size);
        
        return raw;
    }
    
    private boolean proccessOnePacket(SocketChannel channel) throws IOException {
        ByteBuffer bsize = ByteBuffer.allocate(4);
        
        if (!channel.isConnected()) {
            System.out.println("proccessIncoming: channel is closed");
            return false;
        }
        
        int read = channel.read(bsize);
        if(bsize.limit() < 4 || read <= 0) {
            return false;
        }
        bsize.flip();
        int size = bsize.getInt();
        ByteBuffer data = ByteBuffer.allocate(size);
        int pos = 0;
        int count = 0;
        while(pos < size && count < 50) {
            int tmp = channel.read(data);
            if(tmp <0) {
                System.out.println("proccessIncoming - 1");
            }
            pos += tmp;
            count++;
        }
        data.flip();
        
        TransportPacket.Packet input = null;
        input = TransportPacket.Packet.parseDelimitedFrom(new ByteArrayInputStream(data.array()));

        if (input == null) {
            System.out.println("TransportPacket null");
            return false;
        }
        
        NetworkPacket packet = parseTransportPacket(input);
        this.routing.pushNetworkPacket(packet);
        return true;
    }
    
    //public enum IncomingState {KEEPALIVE,SUCCESS,FAILED};
    public boolean proccessIncoming(SocketChannel channel) throws IOException {
        boolean ret = proccessOnePacket(channel);
        boolean flag = true;
        while(ret && flag){
            flag = proccessOnePacket(channel);
        }
        return ret;
    }
        
}
