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
import cotton.internalRouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import static cotton.test.TestRequestQueue.dataArrToStr;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tony
 * @author Magnus
 */
public class TestCommandControl {

    public TestCommandControl() {
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
    public void TestWorkFloodRequestQueue() throws UnknownHostException, IOException {
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

        StatisticsProvider queueManager = queueInstance.getConsole().getProvider(StatType.REQUESTQUEUE);
        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);
        Cotton ser3 = new Cotton(false, gDns);

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);
        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 100);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 1);
        ser3.getServiceRegistation().registerService("result", resFactory, 1000);

        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        //ServiceChainBuilder builder = new ServiceChainBuilder();
        DummyServiceChain.ServiceChainBuilder builder = new DummyServiceChain.ServiceChainBuilder().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
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
            if (i > 600 && (i % 20) == 0) {
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
            }
        }
        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(100);
                StatisticsData[] stats = queueManager.getStatisticsForSubSystem("");
                System.out.println(dataArrToStr(stats));
                if(resFactory.getCounter().intValue() == sentChains)
                    break;
            } catch (InterruptedException ex) {
                //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
            }/*
            if (i == 50) {
                Command com = new Command(StatType.SERVICEHANDLER, "mathpow21", 100);
                Console console = ser2.getConsole();
                byte[] data1 = serializeToBytes(com);
                NetworkPacket packet = NetworkPacket.newBuilder().setData(data1).build();
                console.processCommand(packet);
            }*/
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
                //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        stats = queueManager.getStatisticsForSubSystem("");
        System.out.println(dataArrToStr(stats));

        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        int completedChains = resFactory.getCounter().intValue();
        System.out.println("Completed chains: " + completedChains);
        queueInstance.shutdown();
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        cCotton.shutdown();
        assertTrue(sentChains == completedChains);
    }

    //@Test
    public void TestCommandServiceHandler() throws UnknownHostException {
        Cotton discovery = new Cotton(true, 11243);
        GlobalDnsStub gDns = new GlobalDnsStub();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 11243);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        discovery.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton queue = new Cotton(false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow2");
        queue.setRequestQueueManager(requestQueueManager);
        queue.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Cotton ser1 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow2").into("mathpow2").into("mathpow2");

        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        //ServiceRequest req = client.sendWithResponse(data, chain);
        Console console = ser1.getConsole();
        Console console1 = discovery.getConsole();
        Console console3 = queue.getConsole();
        StatisticsProvider queueStat = console3.getProvider(StatType.REQUESTQUEUE);
        StatisticsProvider discStat = console1.getProvider(StatType.SERVICEDISCOVERY);
        StatisticsProvider serviceHandlerStat = console.getProvider(StatType.SERVICEHANDLER);
        if (queueStat == null) {
            assertTrue(queueStat != null);
        }

        //System.out.println(dataArrToStr(queueStat.getStatisticsForSubSystem("requestQueueNodes")));
        System.out.println(dataArrToStr(discStat.getStatisticsForSubSystem("discoveryNodes")));
        System.out.println(dataArrToStr(discStat.getStatisticsForSubSystem("requestQueueNodes")));
        System.out.println(dataArrToStr(discStat.getStatisticsForSubSystem("serviceNodes")));
        System.out.println(Arrays.toString(serviceHandlerStat.getStatisticsForSubSystem(null)));

        for (int i = 0; i < 1000; i++) {
            chain = new DummyServiceChain().into("mathpow2").into("mathpow2").into("mathpow2").into("mathpow2");
            client.sendToService(data, chain);
        }
        ser1.start();
        StatisticsData s1 = queueStat.getStatistics(new String[]{"mathpow21"});
        StatisticsData s2 = queueStat.getStatistics(new String[]{"mathpow21"});

        System.out.println("\tRequestQueue:");
        System.out.println("\nStats: " + s1.getName() + " : " + "max/current/nodesWaiting" + Arrays.toString(s1.getNumberArray()));
        System.out.println("Stats: " + s2.getName() + " : " + "max/current/nodesWaiting" + Arrays.toString(s2.getNumberArray()));
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        ServiceRequest req = client.sendWithResponse(data, chain);
        if (req != null) {
            byte[] data2 = req.getData();
            int num2 = ByteBuffer.wrap(data2).getInt();
            //System.out.println("result: " + i + " : " + num2);
            num = num2;
        } else {
            System.out.println("Failed req: ");
        }
        System.out.println("result:  : " + num);
        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        discovery.shutdown();
        ser1.shutdown();
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

    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }
}
