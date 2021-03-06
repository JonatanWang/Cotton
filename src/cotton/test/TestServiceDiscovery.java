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
import cotton.test.stubs.NetworkHandlerStub;
import cotton.network.DefaultServiceChain;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.RouteSignal;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceLookup;
import cotton.test.services.MathPowV2;
import cotton.network.NetworkHandler;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.internalrouting.ServiceRequest;
import java.nio.ByteBuffer;
import cotton.internalrouting.DefaultInternalRouting;
import cotton.internalrouting.InternalRoutingClient;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.requestqueue.RequestQueueManager;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.services.ServiceHandler;
import cotton.services.ServiceMetaData;
import cotton.systemsupport.StatType;
import cotton.test.services.GlobalDiscoveryAddress;
import cotton.test.services.MathResult;
import cotton.test.stubs.NetStub;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Magnus
 */
public class TestServiceDiscovery {

    public TestServiceDiscovery() {
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
    public void AnnounceTest() {

    }

    private GlobalServiceDiscovery globalServiceDiscoverySetup() {
        GlobalServiceDiscovery gd = new GlobalServiceDiscovery(null);
        InetSocketAddress local = new InetSocketAddress("127.0.0.1", 3333);
        gd.setNetwork(null, local);
        return gd;
    }

    private Origin originSetup(InetSocketAddress addr, UUID socketLatchID, UUID serviceRequestID) {
        Origin origin = new Origin();
        origin.setAddress(addr);
        origin.setServiceRequestID(serviceRequestID);
        origin.setSocketLatchID(socketLatchID);
        return origin;
    }

    private void localInterfaceSigTest(
            RouteSignal check,
            GlobalServiceDiscovery gd,
            Origin origin, ServiceChain to) {
        RouteSignal sig = gd.getLocalInterface(origin, to);
        assertTrue(check == sig);
    }

    @Test
    public void ActiveServiceLookupGetCapacity() {
        System.out.println("Now running: ActiveServiceLookupGetCapacity");
        ActiveServiceLookup lookup = new ServiceLookup();
        lookup.registerService("hej", null, 10);
        ServiceMetaData service = lookup.getService("hej");
        assertEquals(10, service.getMaxCapacity());
    }

    @Test
    public void ActiveServiceLookupRemove() {
        System.out.println("Now running: ActiveServiceLookupRemove");
        ActiveServiceLookup lookup = new ServiceLookup();

        lookup.registerService("test", null, 10);

        lookup.removeServiceEntry("test");
        assertNull(lookup.getService("test"));
    }

    /**
     * Checking: Origin empty
     */
    @Test
    public void GetLocalInterface01() {
        System.out.println("GetLocalInterface01: Checking: Origin empty");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        Origin origin = originSetup(null, null, null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.LOCALDESTINATION, gd, origin, to);
    }

    /**
     * Checking: ip1 null , socketLatch null ,serviceRequest exist
     */
    @Test
    public void GetLocalInterface02() {
        System.out.println("GetLocalInterface02: Checking: ip1 null, socketLatch null, serviceRequest exist ");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        Origin origin = originSetup(null, null, UUID.randomUUID());
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.ENDPOINT, gd, origin, to);
    }

    /**
     * Checking: ip1 null , socketLatch exist , serviceRequest null
     */
    @Test
    public void GetLocalInterface03() {
        System.out.println("GetLocalInterface03: Checking: ip1 null, socketLatch exist, serviceRequest null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(null, UUID.randomUUID(), null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.BRIDGELATCH, gd, origin, to);
    }

    /**
     * Checking: ip1 != ip2 , port1 == port2
     */
    @Test
    public void GetLocalInterface04() {
        System.out.println("GetLocalInterface04: Checking: ip1 != ip2 , port1 == port2 ");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.2", 3333);
        Origin origin = originSetup(addr, UUID.randomUUID(), null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.RETURNTOORIGIN, gd, origin, to);
    }

    /**
     * Checking: same ip1 == ip2 , port1 != port2
     */
    @Test
    public void GetLocalInterface05() {
        System.out.println("GetLocalInterface05: Checking: ip1 == ip2 , port1 != port2 ");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3335);
        Origin origin = originSetup(addr, UUID.randomUUID(), null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.RETURNTOORIGIN, gd, origin, to);
    }

    /**
     * Checking: ip1 == ip2, socketLatch exist, nat brigde - networkDestination
     */
    @Test
    public void GetLocalInterface06() {
        System.out.println("GetLocalInterface06: Checking: ip1 == ip2, socketLatch exist, nat brigde -> networkDestination");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(addr, UUID.randomUUID(), null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.BRIDGELATCH, gd, origin, to);
    }

    /**
     * Checking: ip1 == ip2, socketLatch null,ServiceRequest null RouteSignal ==
     * LOCALDESTINATION
     */
    @Test
    public void GetLocalInterface07() {
        System.out.println("GetLocalInterface07: Checking: ip1 == ip2, socketLatch null,ServiceRequest null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(addr, null, null);
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.LOCALDESTINATION, gd, origin, to);
    }

    /**
     * Checking: ip1 == ip2, socketLatch null,ServiceRequest exist RouteSignal
     * == ENDPOINT
     */
    @Test
    public void GetLocalInterface08() {
        System.out.println("GetLocalInterface08: Checking: ip1 == ip2, socketLatch null,ServiceRequest exist");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(addr, null, UUID.randomUUID());
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.ENDPOINT, gd, origin, to);
    }

    /**
     * Checking: origin == null RouteSignal == NOTFOUND
     */
    @Test
    public void GetLocalInterface09() {
        System.out.println("GetLocalInterface09: Checking: origin == null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.NOTFOUND, gd, null, to);
    }

    /**
     * Checking: to == null and origin == null RouteSignal == NOTFOUND
     */
    @Test
    public void GetLocalInterface10() {
        System.out.println("GetLocalInterface10: Checking: to == null and origin == null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        ServiceChain to = new DefaultServiceChain();
        localInterfaceSigTest(RouteSignal.NOTFOUND, gd, null, null);
    }

    /**
     * Checking: serviceChain have links
     */
    @Test
    public void GetLocalInterface11() {
        System.out.println("GetLocalInterface11: Checking: serviceChain have links");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        Origin origin = originSetup(null, null, null);
        ServiceChain to = new DefaultServiceChain().into("test");
        localInterfaceSigTest(RouteSignal.LOCALDESTINATION, gd, origin, to);
    }

    /**
     * Checking: whether services can be chained.
     */
    @Test
    public void ServiceChainTest() {
        System.out.println("ServiceChainTest: Checking: Mathpow test");
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        ActiveServiceLookup lookup = new ServiceLookup();
        lookup.registerService("mathpow2", MathPowV2.getFactory(), 10);
        NetworkHandler net = new NetworkHandlerStub(addr);
        ServiceDiscovery global = new GlobalServiceDiscovery(null);
        global.setLocalServiceTable(lookup);
        DefaultInternalRouting internal = new DefaultInternalRouting(net, global);
        ServiceHandler serviceHandler = new ServiceHandler(lookup, internal);
        internal.start();
        new Thread(serviceHandler).start();
        InternalRoutingClient client = internal;
        ServiceChain chain = new DefaultServiceChain().into("mathpow2").into("mathpow2").into("mathpow2");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        ServiceRequest req = client.sendWithResponse(data, chain);

        byte[] data2 = req.getData();
        //System.out.println(data2);
        int num2 = ByteBuffer.wrap(data2).getInt();
        System.out.println("result" + num2);
        internal.stop();
        net.stop();
        serviceHandler.stop();
        global.stop();
        assertTrue(num2 == 256);
    }
    
    /**
     * DiscoverySnapshotTest checks if multiple discovery gets updated
     * subtype to work
     */
    @Test
    public void DiscoverySnapshotTest() throws UnknownHostException, IOException {
        System.out.println("Now running: DiscoverySnapshotTest");
        NetStub tubes = new NetStub();
        int port = new Random().nextInt(25000) + 4000;
        Cotton startDisc = createFakeCotton(tubes, true, port);
        startDisc.start();
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress(port);
        String name1 = "mathpow21";
        String name2 = "mathpow22";
        String name3 = "mathpow23";
        String rname = "result";
        Cotton reqQueue = createFakeCotton(tubes,false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue(name1);
        requestQueueManager.startQueue(name2);
        requestQueueManager.startQueue(name3);
        requestQueueManager.startQueue(rname);
        reqQueue.setRequestQueueManager(requestQueueManager);
        reqQueue.start();
        Cotton ser1 = createFakeCotton(tubes, false, gDns);
        Cotton ser2 = createFakeCotton(tubes, false, gDns);
        Cotton ser3 = createFakeCotton(tubes, false, gDns);

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        ser1.getServiceRegistation().registerService(name1, MathPowV2.getFactory(), 50);
        ser2.getServiceRegistation().registerService(name2, MathPowV2.getFactory(), 50);
        ser2.getServiceRegistation().registerService(name3, MathPowV2.getFactory(), 50);
        ser3.getServiceRegistation().registerService(rname, resFactory, 50);
        
        ser1.start();
        ser2.start();
        ser3.start();
        
        Cotton d1 = createFakeCotton(tubes,true, gDns);
        Cotton d2 = createFakeCotton(tubes,true, gDns);
        Cotton d3 = createFakeCotton(tubes,true, gDns);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        d1.start();
        d2.start();
        d3.start();
        
        String[] availableServices = d3.getAvailableServices();
        d1.shutdown();
        d2.shutdown();
        d3.shutdown();
        
        reqQueue.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        ser3.shutdown();
        startDisc.shutdown();
        if(availableServices.length < 1) {
            assertTrue(false);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("AvailableServices:");
        boolean n1 = false;
        boolean n2 = false;
        boolean n3 = false;
        boolean n4 = false;
        for (String s : availableServices) {
            sb.append(" " + s + ",");
            n1 = n1 || s.equals(name1);
            n2 = n2 || s.equals(name2);
            n3 = n3 || s.equals(name3);
            n4 = n4 || s.equals(rname);
        }
        System.out.println(sb);
        assertTrue(n1 && n2 && n3 && n4);
    }    
    
    /**
     * DiscoveryReannounceTest checks if services reannounce 
     * when primary discovery goes down
     */
    @Test
    public void DiscoveryReannounceTest() throws UnknownHostException, IOException{
        System.out.println("Now running: DiscoverySnapshotTest");
        NetStub tubes = new NetStub();
        int port = new Random().nextInt(25000) + 4000;
        Cotton startDisc = createFakeCotton(tubes, true, port);
        startDisc.start();
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress(port);
         try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton d1 = createFakeCotton(tubes,true, gDns);
        d1.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        String name1 = "mathpow21";
        String name2 = "mathpow22";
        String name3 = "mathpow23";
        String rname = "result";
        Cotton reqQueue = createFakeCotton(tubes,false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue(name1);
        requestQueueManager.startQueue(name2);
        requestQueueManager.startQueue(name3);
        requestQueueManager.startQueue(rname);
        reqQueue.setRequestQueueManager(requestQueueManager);
        reqQueue.start();
        Cotton ser1 = createFakeCotton(tubes, false, gDns);

        AtomicInteger counter = new AtomicInteger(0);
        MathResult.Factory resFactory = (MathResult.Factory) MathResult.getFactory(counter);

        ser1.getServiceRegistation().registerService(name1, MathPowV2.getFactory(), 50);
        ser1.getServiceRegistation().registerService(name2, MathPowV2.getFactory(), 50);
        ser1.getServiceRegistation().registerService(name3, MathPowV2.getFactory(), 50);
        ser1.getServiceRegistation().registerService(rname, resFactory, 50);
        ser1.start();
        LocalServiceDiscovery ld = (LocalServiceDiscovery) ser1.getConsole().getProvider(StatType.DISCOVERY);
         try {
            Thread.sleep(400);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("AvailableServices:");
        String[] availableServices = d1.getConsole().getLocalServices();
        for (String s : availableServices) {
            sb.append(" ").append(s).append(",");
        }
        System.out.println(sb);
        DestinationMetaData destinationForType = ld.getDestinationForType(PathType.DISCOVERY, null);
        System.out.println("Tt: " + destinationForType);
        destinationForType = ld.getDestinationForType(PathType.DISCOVERY, null);
        System.out.println("Tt: " + destinationForType);
        tubes.removeNode((NetworkHandlerStub) startDisc.getNetwork());
        
        startDisc.shutdown();
         try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        SocketAddress[] gda = gDns.getGlobalDiscoveryAddress();
        DestinationMetaData dmd = new DestinationMetaData(gda[0],PathType.DISCOVERY);
        for(int i = 0; i < 5; i++){
            ld.destinationUnreachable(dmd, null);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        availableServices = d1.getConsole().getLocalServices();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        d1.shutdown();
     
        reqQueue.shutdown();
        ser1.shutdown();
        
        if(availableServices.length < 1) {
            assertTrue(false);
        }
        sb = new StringBuilder();
        sb.append("AvailableServices:");
        boolean n1 = false;
        boolean n2 = false;
        boolean n3 = false;
        boolean n4 = false;
        for (String s : availableServices) {
            sb.append(" " + s + ",");
            n1 = n1 || s.equals(name1);
            n2 = n2 || s.equals(name2);
            n3 = n3 || s.equals(name3);
            n4 = n4 || s.equals(rname);
        }
        System.out.println(sb);
        assertTrue(n1 && n2 && n3 && n4);
    }
    
    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, int port) throws UnknownHostException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, stub);
    }

    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, int port, GlobalDiscoveryAddress gda) throws UnknownHostException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, gda, stub);
    }
    
    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, GlobalDiscoveryAddress gda) throws UnknownHostException {
        int port = new Random().nextInt(25000) + 4000;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, gda, stub);
    }
}
