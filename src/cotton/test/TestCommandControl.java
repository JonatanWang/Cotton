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
import cotton.internalRouting.DefaultInternalRouting;
import java.nio.charset.StandardCharsets;
import java.net.SocketAddress;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.internalRouting.DefaultServiceRequest;
import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.network.DestinationMetaData;
import cotton.servicediscovery.AddressPool;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.RouteSignal;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.systemsupport.CommandType;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    //@Test
    public void TestCommandServiceHandler() throws UnknownHostException {
        System.out.println("Now running: TestCommandServiceHandler");
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
        StatisticsProvider discStat = console1.getProvider(StatType.DISCOVERY);
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

        chain = new DummyServiceChain().into("mathpow2").into("mathpow2").into("mathpow2").into("mathpow2");
        ServiceRequest req = client.sendWithResponse(data, chain);
        if (req != null && req.getData() != null) {
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

    @Test
    public void TestServiceRequestTimeout() {
        System.out.println("Now running: TestServiceRequestTimeout");
        GlobalDnsStub stub = new GlobalDnsStub();
        stub.setGlobalDiscoveryAddress(new InetSocketAddress[0]);
        DefaultInternalRouting internalRouting = new DefaultInternalRouting(new NetworkHandlerStub(new InetSocketAddress("127.0.0.1", 16392)), new LocalServiceDiscovery(stub));
        DefaultServiceRequest req = (DefaultServiceRequest) internalRouting.newServiceRequest(new Origin(), 200);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                req.setData("Hej".getBytes());
            }
        }).start();

        byte[] data = req.getData();
        String s = null;
        if (data == null) {
            s = req.getErrorMessage();
        }

        assertTrue("SocketRequest timed out ".equals(s));
    }

    @Test
    public void TestNodesServiceReachabillity() throws UnknownHostException {
        System.out.println("Now running: TestNodesServiceReachabillity");
        Cotton discovery = new Cotton(true, 14490);
        GlobalDnsStub gDns = new GlobalDnsStub();

        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 14490);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();
        try {
            Thread.sleep(900);
        } catch (InterruptedException ex) {
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser1.start();
        ser2.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ser2.shutdown();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Command command = new Command(StatType.DISCOVERY, null, null, 0, CommandType.CHECK_REACHABILLITY);
        byte[] data = null;
        try {
            data = serializeToBytes(command);
        } catch (IOException e) {
        }
        NetworkPacket packet = NetworkPacket.newBuilder().setData(data).build();
        discovery.getConsole().processCommand(packet);
        try {
            Thread.sleep(1050);
        } catch (InterruptedException ex) {
            System.out.println("EXCEPTION INTERRUPTED");
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Console console = ser1.getConsole();
        ServiceDiscovery serviceDiscovery1 = (ServiceDiscovery) console.getProvider(StatType.DISCOVERY);
        RouteSignal route = serviceDiscovery1.getDestination(new DestinationMetaData(), new Origin(), new DummyServiceChain().into("mathpow21"));
        RouteSignal route1 = serviceDiscovery1.getDestination(new DestinationMetaData(), new Origin(), new DummyServiceChain().into("mathpow2"));

        discovery.shutdown();
        ser1.shutdown();
        System.out.println("Routesignal route: " + route);
        System.out.println("Routesignal route1: " + route1);
        assertTrue(route == RouteSignal.NOTFOUND);
    }

    @Test
    public void AddressPoolTest() {
        System.out.println("Now running: AddressPoolTest");
        AddressPool pool = new AddressPool();
        DestinationMetaData dest = new DestinationMetaData(new InetSocketAddress("127.0.0.1", 18762), PathType.DISCOVERY);
        DestinationMetaData dest1 = new DestinationMetaData(new InetSocketAddress("127.0.0.1", 18762), PathType.SERVICE);
        DestinationMetaData dest2 = new DestinationMetaData(new InetSocketAddress("127.0.0.1", 18767), PathType.DISCOVERY);
        DestinationMetaData dest3 = new DestinationMetaData(new InetSocketAddress("125.213.31.92", 18762), PathType.DISCOVERY);
        pool.addAddress(dest);
        boolean b = pool.remove(dest1);
        boolean b1 = pool.remove(dest2);
        boolean b2 = pool.remove(dest3);
        boolean b3 = pool.remove(dest);
        boolean b4 = pool.remove(dest);
        assertTrue(!b && !b1 && !b2 && b3 && !b4);
    }

    @Test
    public void TestQuerySubSystem() throws UnknownHostException {
        System.out.println("Now running: TestQuerySubSystem");
        Cotton discovery = new Cotton(true, 19876);
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress discoveryAddress = new InetSocketAddress(Inet4Address.getLocalHost(), 19876);
        InetSocketAddress gdAddr = discoveryAddress;
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser1.start();
        ser2.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Command command = new Command(StatType.DISCOVERY, "serviceNodes", null, 0, CommandType.STATISTICS_FORSUBSYSTEM);
        command.setQuery(true);
        byte[] data = null;
        try {
            data = serializeToBytes(command);
        } catch (IOException e) {
            System.out.println("IOEXception in testQuerySubSystem");
            e.printStackTrace();
        }
        NetworkPacket packet = NetworkPacket.newBuilder().setData(data).build();
        try {
            Thread.sleep(1050);
        } catch (InterruptedException ex) {
            System.out.println("EXCEPTION INTERRUPTED");
            ex.printStackTrace();

            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        InternalRoutingServiceDiscovery internalRouting = (InternalRoutingServiceDiscovery) ser1.getConsole().getProvider(StatType.INTERNALROUTING);
        ServiceRequest req = internalRouting.sendWithResponse(new DestinationMetaData(discoveryAddress, PathType.COMMANDCONTROL), data, 1000);

        if (req == null) {
            System.out.println("REQ IS NULL");
            assertFalse(true);
        }
        if (req.getData() == null) {
            System.out.println("REQ GETDATA IS NULL");
            assertFalse(true);
        }
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        StatisticsData[] statistics = packetUnpack(req.getData());
        System.out.println("INFORMATION" + Arrays.toString(statistics));
        assertTrue(true);
    }

    private StatisticsData[] packetUnpack(byte[] data) {
        StatisticsData[] statistics = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            statistics = (StatisticsData[]) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return statistics;
    }

}
