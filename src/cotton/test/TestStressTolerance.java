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

import cotton.Cotton;
import cotton.internalrouting.DefaultInternalRouting;
import cotton.internalrouting.InternalRoutingNetwork;
import cotton.internalrouting.InternalRoutingServiceHandler;
import cotton.network.DefaultServiceChain.ServiceChainBuilder;
import cotton.network.NetworkHandler;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.ActiveServiceLookup;
import cotton.services.BridgeServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceHandler;
import cotton.services.ServiceLookup;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;
import cotton.test.services.Result;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author magnus
 */
public class TestStressTolerance {

    public TestStressTolerance() {
    }

    @Test
    public void TestServiceHandlerFlood() throws UnknownHostException, FileNotFoundException, IOException {
        int sentChains = 1000000;
        System.out.println("Now running: TestServiceHandlerFlood: " + sentChains + " chains test");
        AtomicInteger counter = new AtomicInteger(0);
        int dotFreq = 10000;
        int lineFreq = 100000;
        Result.Factory resFactory = (Result.Factory) Result.getFactory(counter, dotFreq, lineFreq);

        ActiveServiceLookup serviceLookup = new ServiceLookup();
        serviceLookup.registerService("mathpow2", MathPowV2.getFactory(), 20);
        serviceLookup.registerService("mathpow21", MathPowV2.getFactory(), 20);
        serviceLookup.registerService("result", resFactory, 100);

        InternalRoutingServiceHandler internal = new InternalRoutingServiceHandler() {
            private BridgeServiceBuffer bridge = new BridgeServiceBuffer();

            @Override
            public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result) {
                if (serviceChain.peekNextServiceName() == null) {
                    return true;
                }
                bridge.add(NetworkPacket
                        .newBuilder()
                        .setOrigin(origin)
                        .setPath(serviceChain)
                        .setData(result)
                        .build());
                return true;
            }

            @Override
            public ServiceBuffer getServiceBuffer() {
                return bridge;
            }

            @Override
            public boolean notifyRequestQueue(String serviceName) {
                return true;
            }
        };

        ServiceHandler serviceHandler = new ServiceHandler(serviceLookup, internal);
        Thread th = new Thread(serviceHandler);
        th.setDaemon(true);
        ServiceBuffer serviceBuffer = internal.getServiceBuffer();
        ServiceChainBuilder build = new ServiceChainBuilder();
        String name1 = "mathpow2";
        String name2 = "mathpow2";
        build.into("mathpow2")
                .into(name1).into(name2)
                .into(name1).into(name2)
                .into(name1).into(name2).into("result");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        for (int i = 0; i < sentChains; i++) {
            NetworkPacket pkt = NetworkPacket.newBuilder()
                    .setOrigin(new Origin())
                    .setPath(build.build())
                    .setData(data)
                    .build();
            serviceBuffer.add(pkt);
        }
        System.out.println("Service buffer loaded with: " + serviceBuffer.size() + " packets");
        serviceHandler.fillBufferChannels();
//        boolean recStarted = serviceHandler.setUsageRecording("mathpow2", 100);
//        if(!recStarted) {
//            System.out.println("Failed to start recording");
//        }
        th.start();
        
        int completedChains = counter.get();
        int timeOut = 0;
        int maxRunTime = 1000;
        double ratio = 1.0 / 100.0;
        int waitTime = (int) ((double) maxRunTime * ratio);
        int loop = maxRunTime - waitTime;
        System.out.println("MaxRunTime: " + maxRunTime + " ratio: " + ratio + " loop: " + loop + " waitTime: " + waitTime);
        long startTime = System.currentTimeMillis();
//        for (int i = 0; i < sentChains; i++) {
//            NetworkPacket pkt = NetworkPacket.newBuilder()
//                    .setOrigin(new Origin())
//                    .setPath(build.build())
//                    .setData(data)
//                    .build();
//            serviceBuffer.add(pkt);
//        }
        while (counter.get() < sentChains && timeOut < loop) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {
            }
            timeOut++;
        }
        long stopTime = System.currentTimeMillis();
        completedChains = counter.get();
//        String[] cmdline = new String[]{"mathpow2", "getUsageRecordingInterval"};
//        Command query = new Command(StatType.SERVICEHANDLER, null, cmdline, 200, CommandType.USAGEHISTORY);
//        query.setQuery(true);
//        StatisticsData<TimeInterval>[] anwser = serviceHandler.processCommand(query);
//        TimeInterval[] data1 = anwser[0].getData();
//        System.out.println("Result:" );
//        printTimeInterval(data1);
        float total = (float) (stopTime - startTime) / 1000.0f;
        System.out.println("Chains completed: " + completedChains + " in: " + total + "[s] , " + (float) completedChains / total + "[chains/sec]");
        serviceHandler.stop();
        assertTrue(sentChains == completedChains);
    }

    private void printTimeInterval(TimeInterval[] data) throws FileNotFoundException, IOException {
        for (int i = 0; i < data.length; i++) {
            System.out.println("" + data[i]);
        }
        FileOutputStream f = new FileOutputStream("MaxTest.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append("" + data[i].getInputCount() + " "+ data[i].getOutputCount() + "\n");
        }
        byte[] data2 = sb.toString().getBytes();
        f.write(data2);
        f.close();
    }
    
    /*
    StringBuilder sb = new StringBuilder();
            sb.append("Name: " + node.getName() + "\n");
            DestinationMetaData[] dest = node.getData();
            sb.append("\tTotal Address:" + dest.length + "\n");
            int count = 0;
            for (DestinationMetaData d : dest) {
                count++;
                sb.append("\t" + count + " : " + d.toString() + "\n");
            }
            byte[] data = sb.toString().getBytes();
            f.write(data);
    
    */
    
    private class NetworkHandlerStub2 implements NetworkHandler {

        InternalRoutingNetwork internal = null;
        InetSocketAddress local;
        InetSocketAddress destAddr;
        private LinkedBlockingQueue<NetworkPacket> output;
        private LinkedBlockingQueue<NetworkPacket> input;

        public NetworkHandlerStub2(SocketAddress local, LinkedBlockingQueue<NetworkPacket> output, LinkedBlockingQueue<NetworkPacket> input, SocketAddress destAddr) {
            this.local = (InetSocketAddress) local;
            this.output = output;
            this.input = input;
            this.destAddr = (InetSocketAddress) destAddr;
        }

        @Override
        public void send(NetworkPacket netPacket, SocketAddress dest) throws IOException {
            if (!this.destAddr.equals((InetSocketAddress) dest)) {
                throw new IOException("Cant reach addr");
            }
            this.output.offer(netPacket);
        }

        @Override
        public void sendKeepAlive(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        }

        @Override
        public SocketAddress getLocalAddress() {
            return this.local;
        }

        @Override
        public void setInternalRouting(InternalRoutingNetwork internal) {
            this.internal = internal;
        }

        AtomicBoolean run = new AtomicBoolean(true);

        @Override
        public void stop() {
            this.run.set(false);
        }

        @Override
        public void run() {
            while (this.run.get()) {
                try {
                    NetworkPacket take = this.input.take();
                    this.internal.pushNetworkPacket(take);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    //@Test
    public void TestInternalRoutingFlood() throws UnknownHostException {
        int sentChains = 1000000;
        System.out.println("Now running: TestServiceHandlerFlood: " + sentChains + " chains test");
        LinkedBlockingQueue<NetworkPacket> link1 = new  LinkedBlockingQueue<>();
        LinkedBlockingQueue<NetworkPacket> link2 = new  LinkedBlockingQueue<>();
        InetSocketAddress local1 = new InetSocketAddress("158.0.0.1",6888);
        InetSocketAddress local2 = new InetSocketAddress("158.0.0.2",7854);
        NetworkHandler net1 = new NetworkHandlerStub2(local1,link1,link2,local2);
        NetworkHandler net2 = new NetworkHandlerStub2(local2,link2,link1,local1);
        
        DefaultInternalRouting internal = new DefaultInternalRouting(net1,null);
        
        
    }
}
