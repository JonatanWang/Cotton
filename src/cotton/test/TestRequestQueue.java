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

import cotton.network.DefaultNetworkHandler;
import cotton.network.DefaultServiceChain;
import cotton.network.DefaultServiceChain.ServiceChainBuilder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import cotton.Cotton;
import cotton.internalrouting.InternalRoutingClient;
import cotton.internalrouting.ServiceRequest;
import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.network.SocketSelectionNetworkHandler;
import cotton.test.services.MathPowV2;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
//import cotton.test.experimental.CloudNetwork;
import cotton.test.services.GlobalDiscoveryAddress;
import cotton.test.services.MathResult;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Magnus
 * @author Tony
 */
public class TestRequestQueue {

    public TestRequestQueue() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void TestRequestQueue() throws UnknownHostException {
        System.out.println("Now running: TestRequestQueue");
        int port = new Random().nextInt(25000) + 4000;
        Cotton discovery = new Cotton(true, newNetHandlerFake(port));
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        Cotton queueInstance = new Cotton(false, gDns,newNetHandlerFake(0));
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton ser1 = new Cotton(false, gDns,newNetHandlerFake(0));
        Cotton ser2 = new Cotton(false, gDns,newNetHandlerFake(0));

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser1.start();
        ser2.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton cCotton = new Cotton(false, gDns,newNetHandlerFake(0));
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DefaultServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");

        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        ServiceRequest[] req = new ServiceRequest[5];

        //ServiceRequest req = client.sendWithResponse(data, chain);
        for (int i = 0; i < req.length; i++) {
            chain = new DefaultServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");
            req[i] = client.sendWithResponse(data, chain);

        }

        for (int i = 0; i < req.length; i++) {
            if (req[i] != null || req[i].getData() != null) {
                byte[] data2 = req[i].getData();

                int num2 = ByteBuffer.wrap(data2).getInt();
                //System.out.println("result: " + i + " : " + num2);
                num = num2;
            } else {
                System.out.println("Failed req: ");
            }
        }
        System.out.println("result:  : " + num);
        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        cCotton.shutdown();
        assertTrue(65536 == num);
    }

    static <T> String dataArrToStr(T[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append("\n" + data[i].toString());

        }
        return sb.toString();
    }

    @Test
    public void TestWorkFloodRequestQueue() throws UnknownHostException {
        System.out.println("Now running: TestWorkFloodRequestQueue");
        int port = /*9999;*/ new Random().nextInt(25000) + 5000;
        Cotton discovery = new Cotton(true, newNetHandlerFake(port));
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton queueInstance = new Cotton(false, gDns, newNetHandlerFake(0));
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        requestQueueManager.startQueue("result");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        StatisticsProvider queueManager = queueInstance.getConsole().getProvider(StatType.REQUESTQUEUE);
        Cotton ser1 = new Cotton(false, gDns, newNetHandlerFake(0));
        Cotton ser2 = new Cotton(false, gDns, newNetHandlerFake(0));
        Cotton ser3 = new Cotton(false, gDns, newNetHandlerFake(0));

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 50);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 50);
        ser3.getServiceRegistation().registerService("result", resFactory, 50);

        Cotton cCotton = new Cotton(false, gDns, newNetHandlerFake(0));
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DefaultServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        //ServiceChainBuilder builder = new ServiceChainBuilder();
        ServiceChainBuilder builder = new ServiceChainBuilder().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
//        ser1.start();
//        ser2.start();
//        ser3.start();
        int sentChains = 1000;
        //ServiceRequest req = client.sendWithResponse(data, chain);
        //chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        for (int i = 0; i < sentChains; i++) {
            //chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
            chain = builder.build();
            client.sendToService(data, chain);
            if (i % 200 == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
            if (i == 600) {
                ser1.start();
                ser2.start();
                ser3.start();
            }
        }

        StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
        System.out.println(dataArrToStr(stats));
        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(10);
                if (resFactory.getCounter().intValue() == sentChains) {
                    break;
                }
            } catch (InterruptedException ex) {
                System.out.println("INTERRUPTED");
                //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        stats = queueManager.getStatisticsForSubSystem("");
        System.out.println(dataArrToStr(stats));
//        chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");
//        ServiceRequest req = client.sendWithResponse(data, chain);
//        if (req != null) {
//            data = req.getData();
//            num = ByteBuffer.wrap(data).getInt();
//            System.out.println("result:  : " + num);
//        } else {
//            System.out.println("req:  : " + req);
//        }
        int completedChains = resFactory.getCounter().intValue();
        System.out.println("Completed chains: " + completedChains);
        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        cCotton.shutdown();
        assertTrue(sentChains == completedChains);
    }

    @Test
    public void TestStatistics() throws UnknownHostException {
        System.out.println("Now running: TestStatistics");
        Cotton discovery = new Cotton(true, 8161);
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 8161);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton queueInstance = new Cotton(false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);
        Cotton ser3 = new Cotton(false, gDns);
        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);
        ser1.getServiceRegistation().registerService("ser01", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser02", MathPowV2.getFactory(), 142);
        ser2.getServiceRegistation().registerService("ser03", resFactory, 11);
        ser1.getServiceRegistation().registerService("ser04", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser05", MathPowV2.getFactory(), 140);
        ser2.getServiceRegistation().registerService("ser06", resFactory, 10);
        ser1.getServiceRegistation().registerService("ser07", MathPowV2.getFactory(), 11);
        ser2.getServiceRegistation().registerService("ser08", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser09", resFactory, 10);
        ser1.start();
        ser2.start();
        ser3.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        StatisticsProvider sd = discovery.getConsole().getProvider(StatType.DISCOVERY);
        StatisticsProvider qm = queueInstance.getConsole().getProvider(StatType.REQUESTQUEUE);
        StatisticsProvider sh1 = ser1.getConsole().getProvider(StatType.SERVICEHANDLER);
        StatisticsProvider sh2 = ser2.getConsole().getProvider(StatType.SERVICEHANDLER);
        StatisticsProvider sh3 = ser3.getConsole().getProvider(StatType.SERVICEHANDLER);
        StatisticsData s1 = qm.getStatistics(new String[]{"mathpow21"});
        StatisticsData s2 = qm.getStatistics(new String[]{"mathpow21"});

        System.out.println("\tRequestQueue:");
        System.out.println("\nStats: " + s1.getName() + " : " + "max/current/nodesWaiting" + Arrays.toString(s1.getNumberArray()));
        System.out.println("Stats: " + s2.getName() + " : " + "max/current/nodesWaiting" + Arrays.toString(s2.getNumberArray()));
        System.out.println("\tServiceHandler:");
        System.out.println(Arrays.toString(sh1.getStatisticsForSubSystem(null)));
        System.out.println(Arrays.toString(sh2.getStatisticsForSubSystem(null)));
        System.out.println(Arrays.toString(sh3.getStatisticsForSubSystem(null)));
        System.out.println("\tDiscovery:");

        System.out.println(dataArrToStr(sd.getStatisticsForSubSystem("discoveryNodes")));
        System.out.println(dataArrToStr(sd.getStatisticsForSubSystem("requestQueueNodes")));
        System.out.println(dataArrToStr(sd.getStatisticsForSubSystem("serviceNodes")));

        StatisticsData[] stat = sh1.getStatisticsForSubSystem(null);
        boolean success = false;
        for (int i = 0; i < stat.length; i++) {
            success = stat[i].getName().equals("ser07");
            if (success) {
                break;
            }

        }
        System.out.println("Completed chains: " + resFactory.getCounter().intValue());
        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        assertTrue(success);
    }

    private NetworkHandler newNetHandlerFake(int port) throws UnknownHostException {
        if (port == 0) {
            port = new Random().nextInt(20000) + 3000;
        }
        //return new NetworkHandlerFake(port);
        return new DefaultNetworkHandler(port);
        //return new SocketSelectionNetworkHandler(port);

//        return new CloudNetwork(port);
    }

    //@Test
    public void TestMassInstances() throws UnknownHostException {
        System.out.println("Now running: TestMassInstances");
        int port = new Random().nextInt(20000) + 5000;
        Cotton discovery = new Cotton(true, newNetHandlerFake(port));
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        int clientCount = 5;
        Cotton[] clientArr = new Cotton[clientCount];
        System.out.println("Starting clients:");
        for (int i = 0; i < clientCount; i++) {
            clientArr[i] = new Cotton(false, gDns, newNetHandlerFake(0));
            clientArr[i].start();
        }
        System.out.print("done\n");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        Cotton queueInstance = new Cotton(false, gDns, newNetHandlerFake(0));
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        requestQueueManager.startQueue("result");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();
        ServiceChainBuilder builder1 = new ServiceChainBuilder().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        ServiceChainBuilder builder2 = new ServiceChainBuilder().into("mathpow21").into("mathpow2").into("mathpow21").into("mathpow2").into("result");

        StatisticsProvider queueManager = queueInstance.getConsole().getProvider(StatType.REQUESTQUEUE);
        int countInst = 40;
        Cotton[] serArr1 = new Cotton[countInst];
        Cotton[] serArr2 = new Cotton[countInst];
        Cotton[] serArr3 = new Cotton[countInst];

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        System.out.println("Starting services:");
        for (int i = 0; i < countInst; i++) {
            serArr1[i] = new Cotton(false, gDns, newNetHandlerFake(0));
            serArr2[i] = new Cotton(false, gDns, newNetHandlerFake(0));
            serArr3[i] = new Cotton(false, gDns, newNetHandlerFake(0));
            serArr1[i].getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
            serArr2[i].getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
            serArr3[i].getServiceRegistation().registerService("result", resFactory, 10);
            serArr1[i].start();
            serArr2[i].start();
            serArr3[i].start();

        }
        System.out.print("done\n");
        System.out.println("Starting fill work queue:");
        int startedNodes = 0;
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < clientArr.length; j++) {
                clientArr[j].getClient().sendToService(data, builder1.build());
                clientArr[j].getClient().sendToService(data, builder2.build());

            }
            if (i % 200 == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
            if (i % 10 == 0) {
                if (startedNodes < countInst) {
//                    serArr1[startedNodes].start();
//                    serArr2[startedNodes].start();
//                    serArr3[startedNodes].start();
                    startedNodes++;
                }

            }
        }
        System.out.print("done\n");
        /*
        for (int i = startedNodes; i < countInst; i++) {
            serArr1[i].start();
            serArr2[i].start();
            serArr3[i].start();
            if (i % 5 == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
        }
         */
        ServiceRequest req = clientArr[0].getClient().sendWithResponse(data, builder2.build());
        data = null;
        num = 2;
        try {
            Thread.sleep(800);
        } catch (InterruptedException ex) {
        }
        if (req != null) {
            data = req.getData();
            if (data == null) {
                System.out.println("reqTime" + req.getErrorMessage());
            }
        } else {
            num = 2068;
        }
        if (data != null) {
            num = ByteBuffer.wrap(data).getInt();

        } else {

            num += 2428;
        }
        System.out.println("result:  : " + num);
        for (int i = 0; i < clientCount; i++) {
            clientArr[i].shutdown();
        }
        for (int i = 0; i < countInst; i++) {
            serArr1[i].shutdown();
            serArr2[i].shutdown();
            serArr3[i].shutdown();
        }
        System.out.println("Completed chains: " + resFactory.getCounter().intValue());
        discovery.shutdown();
        assertTrue(65536 == num);

    }
}
