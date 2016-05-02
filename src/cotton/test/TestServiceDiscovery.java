/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test;

import cotton.network.DummyServiceChain;
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
import cotton.internalRouting.ServiceRequest;
import java.nio.ByteBuffer;
import cotton.internalRouting.DefaultInternalRouting;
import cotton.internalRouting.InternalRoutingClient;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import cotton.services.ServiceHandler;

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

    /**
     * Checking: Origin empty
     */
    @Test
    public void GetLocalInterface01() {
        System.out.println("GetLocalInterface01: Checking: Origin empty");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        Origin origin = originSetup(null, null, null);
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain();
        localInterfaceSigTest(RouteSignal.BRIDGELATCH, gd, origin, to);
    }

    /**
     * Checking: ip1 == ip2, socketLatch null,ServiceRequest null
     * RouteSignal == LOCALDESTINATION
     */
    @Test
    public void GetLocalInterface07() {
        System.out.println("GetLocalInterface07: Checking: ip1 == ip2, socketLatch null,ServiceRequest null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(addr, null, null);
        ServiceChain to = new DummyServiceChain();
        localInterfaceSigTest(RouteSignal.LOCALDESTINATION, gd, origin, to);
    }

    /**
     * Checking: ip1 == ip2, socketLatch null,ServiceRequest exist
     * RouteSignal == ENDPOINT
     */
    @Test
    public void GetLocalInterface08() {
        System.out.println("GetLocalInterface08: Checking: ip1 == ip2, socketLatch null,ServiceRequest exist");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3333);
        Origin origin = originSetup(addr, null, UUID.randomUUID());
        ServiceChain to = new DummyServiceChain();
        localInterfaceSigTest(RouteSignal.ENDPOINT, gd, origin, to);
    }

    /**
     * Checking: origin == null
     * RouteSignal == NOTFOUND
     */
    @Test
    public void GetLocalInterface09() {
        System.out.println("GetLocalInterface09: Checking: origin == null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        ServiceChain to = new DummyServiceChain();
        localInterfaceSigTest(RouteSignal.NOTFOUND, gd, null, to);
    }

    /**
     * Checking: to == null and origin == null
     * RouteSignal == NOTFOUND
     */
    @Test
    public void GetLocalInterface10() {
        System.out.println("GetLocalInterface10: Checking: to == null and origin == null");
        GlobalServiceDiscovery gd = globalServiceDiscoverySetup();
        ServiceChain to = new DummyServiceChain();
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
        ServiceChain to = new DummyServiceChain().into("test");
        localInterfaceSigTest(RouteSignal.LOCALDESTINATION, gd, origin, to);
    }

    /**
    * Checking: whether services can be chained.
    */
   @Test
   public void ServiceChainTest(){

       System.out.println("ServiceChainTest: Checking: Mathpow test");
       InetSocketAddress addr = new InetSocketAddress("127.0.0.1",3333);
       ActiveServiceLookup lookup = new ServiceLookup();
       lookup.registerService("mathpow2", MathPowV2.getFactory(),10);
       NetworkHandler net = new NetworkHandlerStub(addr);
       ServiceDiscovery global = new GlobalServiceDiscovery(null);
       global.setLocalServiceTable(lookup);
       DefaultInternalRouting internal = new DefaultInternalRouting(net,global);
       ServiceHandler serviceHandler = new ServiceHandler(lookup,internal);
       internal.start();
       new Thread(serviceHandler).start();
       InternalRoutingClient client = internal;
       ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow2").into("mathpow2");
       int num = 2;
       byte[] data = ByteBuffer.allocate(4).putInt(num).array();
       ServiceRequest req = client.sendWithResponse(data,chain);

       byte[] data2 = req.getData();
       System.out.println(data2);
       int num2 = ByteBuffer.wrap(data2).getInt();
       System.out.println("result" + num2);
       internal.stop();
       net.stop();
       serviceHandler.stop();
       global.stop();
       assertTrue(num2 == 256);

   }

}
