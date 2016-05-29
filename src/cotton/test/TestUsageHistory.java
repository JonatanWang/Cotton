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
import cotton.internalrouting.InternalRoutingClient;
import cotton.network.DestinationMetaData;
import cotton.network.DefaultServiceChain;
import cotton.network.NetworkPacket;
import cotton.network.PathType;
import org.junit.Test;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import static org.junit.Assert.*;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.test.services.GlobalDiscoveryAddress;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
        System.out.println("Port is:" + new Integer(port).toString());
        //discovery.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);

        //AtomicInteger counter = new AtomicInteger(0);
        // MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);
        //discovery.getServiceRegistation().registerService("result", resFactory, 10);
        RequestQueueManager qm = new RequestQueueManager();
        qm.startQueue("result");
        discovery.setRequestQueueManager(qm);

        discovery.start();

        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();
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
        ServiceChain chain = new DefaultServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");

//        InternalRoutingClient client = cCotton.getClient();
        Console console = queue.getConsole();
        Command command = new Command(StatType.REQUESTQUEUE, null, new String[]{"mathpow21", "setUsageRecordingInterval"}, 100, CommandType.USAGEHISTORY);
        command.setQuery(false);
        byte[] data = null;
        try {
            data = SerializeToBytes.serializeToBytes(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        NetworkPacket packet = NetworkPacket.newBuilder().setData(data).build();
        console.processCommand(packet);
        client.sendToService(data, new DefaultServiceChain().into("mathpow21").into("result"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 500; i++) {
            client.sendToService(data, new DefaultServiceChain().into("mathpow21").into("result"));

        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StatisticsData statistics = requestQueueManager.getStatistics(new String[]{"mathpow21", "getUsageRecordingInterval", "0", "20"});

        TimeInterval[] interval = (TimeInterval[]) statistics.getData();
        System.out.println("Statistics name: " + statistics.getName() + " TIMEINTERVAL:" + intervalToString("\t", interval, "\n"));
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
        System.out.println("Port is:" + port);
        InetSocketAddress addr = new InetSocketAddress(ip, port);
        Cotton discovery = new Cotton(true, port);
        String name = "result";
        String pow = "mathpow21";
        
        discovery.start();
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        gDns.setGlobalDiscoveryAddress(arr);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cotton queueManager = new Cotton(false, gDns);
        RequestQueueManager qm = new RequestQueueManager();
        qm.startQueue(name);
        qm.startQueue(pow);
        queueManager.setRequestQueueManager(qm);
        queueManager.start();
        
        try {
            Thread.sleep(200);
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
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cotton c = new Cotton(false, gDns);
        c.start();
        Console cl = c.getConsole();
        Command cmd = new Command(StatType.REQUESTQUEUE, null, new String[]{pow, "setUsageRecordingInterval"}, 200, CommandType.USAGEHISTORY);
        DestinationMetaData dest = new DestinationMetaData(queueManager.getNetwork().getLocalAddress(), PathType.COMMANDCONTROL);
        try {
            cl.sendCommand(cmd, dest);
        } catch (IOException ex) {
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        InternalRoutingClient client = c.getClient();
        ServiceChain chain = new DefaultServiceChain().into(pow).into(name).into(pow).into(pow).into(name);
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        
        client.sendToService(data, chain);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] cmdline = new String[]{pow, "getUsageRecordingInterval","" + 0,"" + 1000};
        Command query = new Command(StatType.REQUESTQUEUE, null, cmdline, 200, CommandType.USAGEHISTORY);
        StatisticsData<TimeInterval>[] res = null;
        try {
            res = cl.sendQueryCommand(query, dest);
        } catch (IOException ex) {
            System.out.println("send2");
        }
        if (res == null || res.length < 1) {
            System.out.println("getStatData: fail");

        }

        Command stopCmd = new Command(StatType.REQUESTQUEUE, null, new String[]{pow, "stopUsageRecording"}, 0, CommandType.USAGEHISTORY);

        try {

            cl.sendCommand(stopCmd, dest);
        } catch (IOException ex) {
            System.out.println("send2");
            return;
        }
        System.out.println("res: size" + res.length);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("query returned empty",res.length > 0);
        System.out.println("Result: \n" + res[0].toString());

        ser1.shutdown();
        ser2.shutdown();
        discovery.shutdown();
        c.shutdown();
        assertTrue(true);
    }

    @Test
    public void TestServiceStat() throws UnknownHostException, IOException {
        System.out.println("Now running: TestServiceStat");
        int discPort = new Random().nextInt(25000) + 4000;
        int servicePort = new Random().nextInt(25000) + 4000;
        Cotton disc = new Cotton(true, discPort);
        disc.start();

        GlobalDiscoveryAddress dnsStub = getDnsStub(null, discPort);
        String serviceName = "mathpow21";
        Cotton serv = new Cotton(false, servicePort, dnsStub);
        serv.getServiceRegistation().registerService(serviceName, MathPowV2.getFactory(), 10);
        serv.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), servicePort);
        DestinationMetaData destination = new DestinationMetaData(addr, PathType.COMMANDCONTROL);

        int newAmount = 66;
        Cotton client = new Cotton(false, dnsStub);
        client.start();
        Console console = client.getConsole();
        String[] queryRequest = new String[]{serviceName, "isSampling"};
        Command isSample = new Command(StatType.SERVICEHANDLER, serviceName, queryRequest, 0, CommandType.USAGEHISTORY);
        StatisticsData[] sampleRes = console.sendQueryCommand(isSample, destination);
        String startNumb = null;
        String endNumb = null;
        if (sampleRes != null && sampleRes.length > 0) {
            Integer startPoint = sampleRes[0].getNumberArray()[2];
            startNumb = startPoint.toString();
            Integer endPoint = startPoint + 500;
            endNumb = endPoint.toString();
        } else {
            assertTrue("TestServiceStat: isSampling failed",false);
        }

        Command startRecording = new Command(StatType.SERVICEHANDLER, null, new String[]{serviceName, "setUsageRecordingInterval",startNumb,endNumb}, 100, CommandType.USAGEHISTORY);
        console.sendCommand(startRecording, destination);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int num = 5;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        int sendCount = 200;
        InternalRoutingClient c = client.getClient();
        for (int i = 0; i < sendCount; i++) {
            c.sendToService(data, new DefaultServiceChain().into(serviceName));
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        String[] getRec = new String[]{serviceName, "getUsageRecordingInterval", startNumb, endNumb};
        //Command query = new Command(type, null, cmdline, 200, CommandType.STATISTICS_FORSYSTEM);
        Command getRecording = new Command(StatType.SERVICEHANDLER, null, getRec, 200, CommandType.USAGEHISTORY);

        StatisticsData<TimeInterval>[] recording = console.sendQueryCommand(getRecording, destination);
        if (recording == null) {
            System.out.println("TestServiceStat: query returnd null");
            assertTrue(false);
        } else if (recording.length <= 0) {
            System.out.println("TestServiceStat: query returnd empty result");
            assertTrue(false);
        }
        
        
        Command stopRecording = new Command(StatType.SERVICEHANDLER, null, new String[]{serviceName, "stopUsageRecording"}, 0, CommandType.USAGEHISTORY);
        console.sendCommand(stopRecording, destination);
        
        TimeInterval[] interval = recording[0].getData();
        long taskDone = 0;
        for (int i = 0; i < interval.length; i++) {
            taskDone += interval[i].getOutputCount();
        }
        //System.out.println("Statistics name: " + serviceName + " TIMEINTERVAL:" + intervalToString("\t", interval, "\n"));
        
        System.out.println("TestServiceStat:task done:" + taskDone + "==" + sendCount);
        client.shutdown();
        serv.shutdown();
        disc.shutdown();
        assertTrue(taskDone == sendCount);
    }

    private static GlobalDiscoveryAddress getDnsStub(String dest, int port) throws UnknownHostException {
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();
        InetSocketAddress gdAddr = null;
        if (dest == null) {
            gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
            System.out.println("discAddr:" + Inet4Address.getLocalHost().toString() + " port: " + port);
        } else {
            gdAddr = new InetSocketAddress(dest, port);
        }
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
