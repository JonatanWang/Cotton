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
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DestinationMetaData;
import cotton.network.DummyServiceChain;
import cotton.network.NetworkPacket;
import cotton.network.NetworkPacket.NetworkPacketBuilder;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.SocketLatch;
import java.nio.ByteBuffer;
import org.junit.Test;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import static org.junit.Assert.*;
import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 * @author Tony
 *
 */
public class TestUsageHistory {

    public TestUsageHistory() {
    }

    @Test
    public void TestQueueUsage() throws UnknownHostException {
        System.out.println("TestQueueUsage");

        int port = new Random().nextInt(25000) + 5000;
        Cotton discovery = new Cotton(true, port);
        //discovery.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);

        //AtomicInteger counter = new AtomicInteger(0);
        // MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);
        //discovery.getServiceRegistation().registerService("result", resFactory, 10);
        RequestQueueManager qm = new RequestQueueManager();
        qm.startQueue("result");
        discovery.setRequestQueueManager(qm);

        discovery.start();

        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        gDns.setGlobalDiscoveryAddress(arr);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton queue = new Cotton(true, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        queue.setRequestQueueManager(requestQueueManager);
        //queue.getServiceRegistation().registerService("result", (MathResult.Factory) MathResult.getFactory(new AtomicInteger(0)), 10);

        queue.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);
        Cotton ser3 = new Cotton(false, gDns);

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser3.getServiceRegistation().registerService("result", resFactory, 10);

        ser1.start();
        ser2.start();
        ser3.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");

//        InternalRoutingClient client = cCotton.getClient();
        Console console = queue.getConsole();
        Command command = new Command(StatType.REQUESTQUEUE, null, new String[]{"mathpow21", "setUsageRecordingInterval"}, 100, CommandType.RECORD_USAGEHISTORY);
        command.setQuery(false);
        byte[] data = null;
        try {
            data = SerializeToBytes.serializeToBytes(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        NetworkPacket packet = NetworkPacket.newBuilder().setData(data).build();
        console.processCommand(packet);
        client.sendToService(data, new DummyServiceChain().into("mathpow21").into("result"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 500; i++) {
            client.sendToService(data, new DummyServiceChain().into("mathpow21").into("result"));

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StatisticsData statistics = requestQueueManager.getStatistics(new String[]{"mathpow21", "getUsageRecordingInterval", "0", "20"});

        TimeInterval[] interval = (TimeInterval[]) statistics.getData();
        System.out.println("Statistics name: " + statistics.getName() + " TIMEINTERVAL:" + intervalToString("\t",interval,"\n"));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        queue.shutdown();
        cCotton.shutdown();
        discovery.shutdown();
        assertTrue(true);
      
    }
    
    private String intervalToString(String prefix, TimeInterval[] interval, String sufix) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < interval.length; i++) {
            b.append(prefix + interval[i] + sufix);
        }
        return b.toString();
    }

    @Test
    public void TestUsageHistoryQuery() throws UnknownHostException {
        System.out.println("TestQueueUsage");
        InetAddress ip = Inet4Address.getLocalHost();
        int port = new Random().nextInt(25000) + 5000;
        InetSocketAddress addr = new InetSocketAddress(ip, port);
        Cotton discovery = new Cotton(true, port);
        //discovery.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);

        //AtomicInteger counter = new AtomicInteger(0);
        // MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);
        //discovery.getServiceRegistation().registerService("result", resFactory, 10);
        String name = "result";
        String pow = "mathpow21";
        RequestQueueManager qm = new RequestQueueManager();
        qm.startQueue(name);
        qm.startQueue(pow);
        discovery.setRequestQueueManager(qm);

        discovery.start();
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        gDns.setGlobalDiscoveryAddress(arr);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        ser1.getServiceRegistation().registerService(name, resFactory, 10);
        ser2.getServiceRegistation().registerService(pow, MathPowV2.getFactory(), 10);

        ser1.start();
        ser2.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cotton c = new Cotton(false, gDns);
        c.start();
        Console cl = c.getConsole();
        Command cmd = new Command(StatType.REQUESTQUEUE, null, new String[]{pow, "setUsageRecordingInterval"}, 200, CommandType.RECORD_USAGEHISTORY);
        DestinationMetaData dest = new DestinationMetaData(addr, PathType.COMMANDCONTROL);
        try {
            cl.sendCommand(cmd, dest);
        } catch (IOException ex) {
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        InternalRoutingClient client = c.getClient();
        ServiceChain chain = new DummyServiceChain().into(pow).into(name).into(pow).into(pow).into(name);
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        client.sendToService(data, chain);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] cmdline = new String[]{pow, "getUsageRecordingInterval"};
        Command query = new Command(StatType.REQUESTQUEUE, null, cmdline, 200, CommandType.STATISTICS_FORSYSTEM);
        StatisticsData<TimeInterval>[] res = null;
        try {
            res = cl.sendQueryCommand(query, dest);
        } catch (IOException ex) {
            System.out.println("send2");
        }
        if (res == null || res.length < 1) {
            System.out.println("getStatData: fail");

        }

        Command stopCmd = new Command(StatType.REQUESTQUEUE, null, new String[]{pow, "stopUsageRecording"}, 0, CommandType.RECORD_USAGEHISTORY);

        try {

            cl.sendCommand(stopCmd, dest);
        } catch (IOException ex) {
            System.out.println("send2");
            return;
        }
        System.out.println("res: size" + res.length);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Result: \n" + res[0].toString());

        ser1.shutdown();
        ser2.shutdown();
        discovery.shutdown();
        c.shutdown();
    }

}
