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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all of the network output
 *
 * @author Jonathan Kåhre
 */
public class NetworkOutput implements Runnable {
    private BlockingQueue<OutputPacket> queue;
    private ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels;
    private AtomicBoolean run;
    private SocketSelectionNetworkHandler handler;
    private ByteBuffer size;

    /**
     * Returns a <code>NetworkOutput</code> using the specified parameters.
     *
     * @param handler The <code>NetworkHandler</code> in charge of input
     * @param openChannels The collection of open <code>SocketChannels</code>.
     * @param queue The network output queue.
     */
    public NetworkOutput(SocketSelectionNetworkHandler handler, ConcurrentHashMap<InetSocketAddress, SocketChannel> openChannels, BlockingQueue<OutputPacket> queue) {
        this.handler = handler;
        this.openChannels = openChannels;
        this.queue = queue;
        run = new AtomicBoolean(true);
        size = ByteBuffer.allocate(4);
    }

    @Override
    public void run() {
        SocketChannel sendChannel = null;
        SocketAddress dest = null;
        while(run.get()){
            try {
                OutputPacket p = queue.take();
                NetworkPacket packet = p.getPacket();
                dest = p.getDestination();
                size.clear();
                //TransportPacket.Packet tp = buildTransportPacket(packet, p.isKeepAlive());
                //ByteBuffer output = writeOutput(tp);
                ByteBuffer output = packet.getSerializedData();
                if(output == null)
                    output = writeOutput(buildTransportPacket(packet, p.isKeepAlive()));

                sendChannel = null;

                //System.out.println(packet.getType()+" packet of "+output.capacity()+" bytes outgoing from: "+getLocalAddress()+" normal send.");
                size.putInt(0, output.capacity());

                if ((sendChannel = openChannels.get((InetSocketAddress) dest)) != null) {
                    sendChannel.write(size);
                    size.clear();
                    size.putInt(0, packet.getType().ordinal());
                    sendChannel.write(size);
                    size.clear();
                    sendChannel.write(output);
                } else {
                    sendChannel = SocketChannel.open();
                    sendChannel.connect(dest);
                    sendChannel.configureBlocking(false);
                    while (!sendChannel.finishConnect())
                        System.out.println("Not finished connecting");

                    sendChannel.write(size);
                    size.clear();
                    size.putInt(0, packet.getType().ordinal());
                    sendChannel.write(size);
                    size.clear();
                    sendChannel.write(output);
                    handler.registerChannel(sendChannel);
                    openChannels.putIfAbsent((InetSocketAddress) dest, sendChannel);
                }
            }catch(IOException e){
                e.printStackTrace();
                if(sendChannel != null) {
                    if(dest != null) {
                        openChannels.remove((InetSocketAddress)dest, sendChannel);
                    }
                    try {
                        sendChannel.close();
                    } catch (IOException ex) {
                        Logger.getLogger(NetworkOutput.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private ByteBuffer writeOutput(TransportPacket.Packet tp) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        tp.writeDelimitedTo(baos);
        ByteBuffer b = ByteBuffer.wrap(baos.toByteArray());
        //ByteBuffer b = ByteBuffer.wrap(tp.toByteArray());

        return b;
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

        //builder.setPathtype(TransportPacket.Packet.PathType.valueOf(input.getType().toString()));

        return builder;
    }

    public TransportPacket.Packet buildTransportPacket(NetworkPacket input, boolean keepAlive) throws IOException{
        TransportPacket.Packet.Builder builder = parseNetworkPacket(input);
        builder.setKeepalive(keepAlive);
        return builder.build();
    }

    /**
     * Stops this output handler
     */
    public void stop(){
        this.run.set(false);
    }
}
