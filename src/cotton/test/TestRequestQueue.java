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
import cotton.network.DummyServiceChain;
import cotton.network.DummyServiceChain.ServiceChainBuilder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import cotton.Cotton;
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.ServiceRequest;
import cotton.network.ServiceChain;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.test.services.MathPowV2;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathResult;
import java.util.Arrays;

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
        Cotton discovery = new Cotton(true, 11765);
        GlobalDnsStub gDns = new GlobalDnsStub();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 11765);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        Cotton queueInstance = new Cotton(false, gDns);
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

        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser1.start();
        ser2.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");

        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        ServiceRequest[] req = new ServiceRequest[5];

        //ServiceRequest req = client.sendWithResponse(data, chain);
        for (int i = 0; i < req.length; i++) {
            chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");
            req[i] = client.sendWithResponse(data, chain);

        }

        for (int i = 0; i < req.length; i++) {
            if (req[i] != null) {
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
        Cotton discovery = new Cotton(true, 8266);
        GlobalDnsStub gDns = new GlobalDnsStub();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 8266);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        Cotton queueInstance = new Cotton(false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        requestQueueManager.startQueue("result");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        StatisticsProvider queueManager = queueInstance.getConsole().getQueueManager();
        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);
        Cotton ser3 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 100);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 100);
        ser3.getServiceRegistation().registerService("result", MathResult.getFactory(), 1000);

        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        //ServiceChainBuilder builder = new ServiceChainBuilder();
        ServiceChainBuilder builder = new ServiceChainBuilder().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        //ServiceRequest req = client.sendWithResponse(data, chain);
        //chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        for (int i = 0; i < 1000; i++) {
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        stats = queueManager.getStatisticsForSubSystem("");
        System.out.println(dataArrToStr(stats));
        chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");
        ServiceRequest req = client.sendWithResponse(data, chain);
        if (req != null) {
            data = req.getData();
            num = ByteBuffer.wrap(data).getInt();
            System.out.println("result:  : " + num);
        } else {
            System.out.println("req:  : " + req);
        }

        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        cCotton.shutdown();
        assertTrue(65536 == num);
    }

    //@Test
    public void TestStatistics() throws UnknownHostException {
        Cotton discovery = new Cotton(true, 8161);
        GlobalDnsStub gDns = new GlobalDnsStub();

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

        ser1.getServiceRegistation().registerService("ser01", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser02", MathPowV2.getFactory(), 142);
        ser2.getServiceRegistation().registerService("ser03", MathResult.getFactory(), 11);
        ser1.getServiceRegistation().registerService("ser04", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser05", MathPowV2.getFactory(), 140);
        ser2.getServiceRegistation().registerService("ser06", MathResult.getFactory(), 10);
        ser1.getServiceRegistation().registerService("ser07", MathPowV2.getFactory(), 11);
        ser2.getServiceRegistation().registerService("ser08", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("ser09", MathResult.getFactory(), 10);
        ser1.start();
        ser2.start();
        ser3.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        StatisticsProvider sd = discovery.getConsole().getServiceDiscvery();
        StatisticsProvider qm = queueInstance.getConsole().getQueueManager();
        StatisticsProvider sh1 = ser1.getConsole().getServiceHandler();
        StatisticsProvider sh2 = ser2.getConsole().getServiceHandler();
        StatisticsProvider sh3 = ser3.getConsole().getServiceHandler();
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
        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        assertTrue(success);
    }

    //@Test
    public void TestMassInstances() throws UnknownHostException {
        Cotton discovery = new Cotton(true, 7159);
        GlobalDnsStub gDns = new GlobalDnsStub();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 7159);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        int clientCount = 5;
        Cotton[] clientArr = new Cotton[clientCount];
        System.out.println("Starting clients:");
        for (int i = 0; i < clientCount; i++) {
            clientArr[i] = new Cotton(false, gDns);
            clientArr[i].start();
        }
        System.out.print("done\n");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        Cotton queueInstance = new Cotton(false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();
        ServiceChainBuilder builder1 = new ServiceChainBuilder().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        ServiceChainBuilder builder2 = new ServiceChainBuilder().into("mathpow21").into("mathpow2").into("mathpow21").into("mathpow2").into("result");

        StatisticsProvider queueManager = queueInstance.getConsole().getQueueManager();
        int countInst = 10;
        Cotton[] serArr1 = new Cotton[countInst];
        Cotton[] serArr2 = new Cotton[countInst];
        Cotton[] serArr3 = new Cotton[countInst];

        System.out.println("Starting services:");
        for (int i = 0; i < countInst; i++) {
            serArr1[i] = new Cotton(false, gDns);
            serArr2[i] = new Cotton(false, gDns);
            serArr3[i] = new Cotton(false, gDns);
            serArr1[i].getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
            serArr2[i].getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
            serArr3[i].getServiceRegistation().registerService("result", MathResult.getFactory(), 10);

        }
        System.out.print("done\n");
        System.out.println("Starting fill work queue:");
        for (int i = 0; i < 500; i++) {
            for (int j = 0; j < clientArr.length; j++) {
                clientArr[j].getClient().sendToService(data, builder1.build());
                clientArr[j].getClient().sendToService(data, builder2.build());

            }
            if (i % 200 == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
        }
        System.out.print("done\n");

        for (int i = 0; i < countInst; i++) {
            serArr1[i].start();
            serArr2[i].start();
            serArr3[i].start();
            if (i % 5 == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
        }

        ServiceRequest req = clientArr[0].getClient().sendWithResponse(data, builder2.build());
        data = req.getData();
        num = ByteBuffer.wrap(data).getInt();
        System.out.println("result:  : " + num);
        for (int i = 0; i < 10; i++) {
            clientArr[i].shutdown();
        }
        for (int i = 0; i < countInst; i++) {
            serArr1[i].shutdown();
            serArr2[i].shutdown();
            serArr3[i].shutdown();
        }
        discovery.shutdown();
        assertTrue(65536 == num);

    }
}
