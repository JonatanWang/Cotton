package cotton.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jonathan on 18/05/16.
 */
public class NetworkOutput implements Runnable {
    private BlockingQueue<OutputPacket> queue;
    private ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels;
    private AtomicBoolean run;
    private SocketSelectionNetworkHandler handler;

    public NetworkOutput(SocketSelectionNetworkHandler handler, ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels, BlockingQueue<OutputPacket> queue) {
        this.handler = handler;
        this.openChannels = openChannels;
        this.queue = queue;
        run = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        while(run.get()){
            try {
                OutputPacket p = queue.take();
                NetworkPacket packet = p.getPacket();
                SocketAddress dest = p.getDestination();

                TransportPacket.Packet tp = buildTransportPacket(packet, p.isKeepAlive());

                ByteBuffer output = writeOutput(tp);

                SocketChannel sendChannel = null;

                //System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" normal send.");

                if ((sendChannel = openChannels.get((InetSocketAddress) dest)) != null) {
                    ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
                    synchronized (sendChannel) {
                        sendChannel.write(size);

                        sendChannel.write(output);
                    }
                } else {
                    sendChannel = SocketChannel.open();
                    sendChannel.connect(dest);
                    sendChannel.configureBlocking(false);
                    while (!sendChannel.finishConnect())
                        System.out.println("Not finished connecting");

                    ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
                    synchronized (sendChannel) {
                        sendChannel.write(size);

                        sendChannel.write(output);
                    }
                    handler.registerChannel(sendChannel);
                    openChannels.putIfAbsent((InetSocketAddress) dest, sendChannel);
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
            /* SEND:
                    TransportPacket.Packet tp = buildTransportPacket(packet);

                    ByteBuffer output = writeOutput(tp);

                    SocketChannel sendChannel = null;

                    //System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" normal send.");

                    if ((sendChannel = openChannels.get((InetSocketAddress) dest)) != null) {
                        ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
                        synchronized (sendChannel) {
                            sendChannel.write(size);

                            sendChannel.write(output);
                        }
                    } else {
                        sendChannel = SocketChannel.open();
                        sendChannel.connect(dest);
                        sendChannel.configureBlocking(false);
                        while (!sendChannel.finishConnect())
                            System.out.println("Not finished connecting");

                        ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
                        synchronized (sendChannel) {
                            sendChannel.write(size);

                            sendChannel.write(output);
                        }
                        registerChannel(sendChannel);
                        selector.wakeup();
                        openChannels.putIfAbsent((InetSocketAddress) dest, sendChannel);
                    }*/

            /*System.out.println("Keepalive send!");

        TransportPacket.Packet tp = buildTransportPacket(packet, true);

        ByteBuffer output = writeOutput(tp);

        SocketChannel sendChannel = null;

        //System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" keepalive send.");

        if((sendChannel = openChannels.get((InetSocketAddress) dest)) != null) {
            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
            synchronized (sendChannel) {
                sendChannel.write(size);

                sendChannel.write(output);
            }
        } else {
            sendChannel = SocketChannel.open();
            sendChannel.connect(dest);
            sendChannel.configureBlocking(false);

            ByteBuffer size = ByteBuffer.allocate(4).putInt(0, output.capacity());
            synchronized (sendChannel) {
                sendChannel.write(size);

                sendChannel.write(output);
            }
            registerChannel(sendChannel);
            selector.wakeup();
            openChannels.putIfAbsent((InetSocketAddress)dest, sendChannel);
        }*/
        }
    }

    private ByteBuffer writeOutput(TransportPacket.Packet tp) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        tp.writeDelimitedTo(baos);
        ByteBuffer b = ByteBuffer.wrap(baos.toByteArray());

        return b;
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

    public void stop(){
        this.run.set(false);
    }
}
