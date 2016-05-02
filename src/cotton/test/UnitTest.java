package cotton.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import cotton.Cotton;
import cotton.DeprecatedCotton;
import cotton.network.ClientNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.ServiceChain;
import cotton.services.CloudContext;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.DummyServiceChain;
import cotton.services.DeprecatedServiceHandler;
import cotton.services.DeprecatedServiceMetaData;
import cotton.services.DeprecatedServicePacket;
import cotton.test.services.MathPow2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.network.*;
import cotton.servicediscovery.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import cotton.network.DeprecatedNetworkHandler;
import cotton.network.DeprecatedServiceRequest;
import cotton.services.DeprecatedService;
import cotton.services.DeprecatedServiceBuffer;
import cotton.services.DeprecatedServiceFactory;
import cotton.services.DeprecatedActiveServiceLookup;
import cotton.network.DeprecatedServiceConnection;

public class UnitTest {
    public UnitTest() {
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
    public void ActiveServiceLookupGetCapacity(){
        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("hej", null, 10);
        DeprecatedServiceMetaData service = lookup.getService("hej");
        assertEquals(10,service.getMaxCapacity());
    }

    @Test
    public void ActiveServiceLookupRemove(){
        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", null, 10);

        lookup.removeServiceEntry("test");
        assertNull(lookup.getService("test"));
    }

    private class TestService implements DeprecatedService{

        @Override
        public byte[] execute(CloudContext ctx, DeprecatedServiceConnection from, byte[] data, ServiceChain to) {
            String in = "fail";

            ObjectInputStream inStream;
            in = new String(data);

            System.out.println(in);

            return (in+"hej").getBytes();
        }

    }

    public class TestFactory implements DeprecatedServiceFactory {

        @Override
        public DeprecatedService newService() {
            return new TestService();
        }

    }

    private class DummyBufferStuffer{
        private DeprecatedServicePacket servicePacket;
        DeprecatedServiceBuffer serviceBuffer;
        public DummyBufferStuffer(DeprecatedServiceBuffer buffer, DeprecatedServiceConnection from, byte[] data, ServiceChain to){
            this.serviceBuffer = buffer;
            //try{
                /*
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream outStream = new PipedOutputStream(in);
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                objectOutStream.writeObject(data);
                objectOutStream.close();
                */
                this.servicePacket = new DeprecatedServicePacket(from, data, to);
                /*}catch(IOException e){
                System.out.println("io exeption");
                e.printStackTrace();
                }*/
        }

        public void fillBuffer(){
            serviceBuffer.add(this.servicePacket);
        }
    }

    private class Threadrun implements Runnable {

        private DeprecatedServiceHandler handler = null;
        public Threadrun(DeprecatedServiceHandler handler) {
            this.handler = handler;
        }
        @Override
        public void run() {
            System.out.println("text2");
            //this.handler.start();
            this.handler.run();
        }

    }

    @Test
    public void ThreadPoolTest(){
        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", new TestFactory(), 10);
        DeprecatedServiceDiscovery discovery = new DefaultLocalServiceDiscovery(lookup);

        DeprecatedNetworkHandler net = null;

        try {
            net = new DeprecatedDefaultNetworkHandler(discovery);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        DeprecatedServiceHandler handler = new DeprecatedServiceHandler(lookup,net);

        ServiceChain to1 = new DummyServiceChain("test");
        to1.addService("test");
        to1.addService("test");
        to1.addService("test");
        try{
            net.sendToService("hej".getBytes(), to1, null);

            ServiceChain to2 = new DummyServiceChain("test");
            net.sendToService("service2".getBytes(), to2, null);
        }catch(IOException e){
            e.printStackTrace();
        }

        Thread th = new Thread(new Threadrun(handler));
        th.start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        handler.stop();
        System.out.println("text");
        assertNull(net.nextPacket());
    }

    @Test
    public void CottonClientTest(){
        DeprecatedCotton cotton = null;
        try {
            cotton = new DeprecatedCotton();
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        DeprecatedActiveServiceLookup reg = cotton.getServiceRegistation();
        reg.registerService("MathPow2", MathPow2.getFactory(), 8);
        cotton.start();

        ClientNetwork net = cotton.getClientNetwork();

        ServiceChain chain = new DummyServiceChain()
                .into("MathPow2").into("MathPow2")
                .into("MathPow2").into("MathPow2");

        DeprecatedServiceRequest jobId = null;
        try{
            jobId = net.sendToService(ByteBuffer.allocate(4).putInt(2).array(), chain);
        }catch(IOException e){
            e.printStackTrace();
        }

        int result = ByteBuffer.wrap(jobId.getData()).getInt();

        System.out.println(result);
        cotton.shutdown();
        assertTrue(65536 == result);
     }

    @Test
    public void LocalServiceDiscoveryLookup() {
        System.out.println("LocalServiceDiscoveryLookup");

        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("test", new TestFactory(), 10);
        DeprecatedServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);
        DeprecatedServiceConnection dest = new DeprecatedDefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain().into("test");
        assertTrue(RouteSignal.LOCALDESTINATION == local.getDestination(dest, null, chain));
    }

    @Test
    public void LocalServiceDiscoveryLookupTwoInputs() {
        System.out.println("LocalServiceDiscoveryLookupTwoInputs, only two imputs");

        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("test", new TestFactory(), 10);
        DeprecatedServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);
        DeprecatedServiceConnection dest = new DeprecatedDefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain().into("test");
        assertTrue(RouteSignal.LOCALDESTINATION == local.getDestination(dest, chain));
    }

    @Test
    public void LocalServiceDiscoveryLookupNullCheck() {
        System.out.println("LocalServiceDiscoveryLookupNullCheck, checks serviceChain null");

        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("test", new TestFactory(), 10);
        DeprecatedServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);
        DeprecatedServiceConnection dest = new DeprecatedDefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain();
        assertTrue(RouteSignal.NOTFOUND == local.getDestination(dest, null, chain));
    }

    @Test
    public void LocalServiceDiscoveryLookupNullInput() {
        System.out.println("LocalServiceDiscoveryLookupNullInput, checks destinatin null");

        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("test", new TestFactory(), 10);
        DeprecatedServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);
        DeprecatedServiceConnection dest = new DeprecatedDefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain().into("test");
        assertTrue(RouteSignal.NOTFOUND == local.getDestination(null, chain));
    }

    @Test
    public void LocalServiceDiscoveryUpdate(){
        System.out.println("LocalServiceDiscoveryUpdate, testing discoveryUpdate");

        InetSocketAddress name = null;
        try{
            name = new InetSocketAddress(InetAddress.getByName(null), 1234);
        }catch(IOException e){}

        DiscoveryProbe prob = new DiscoveryProbe("test", name);

        DiscoveryPacket pack = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE);
        //pack.setPacketType(DiscoveryPacketType.DISCOVERYRESPONSE);
        pack.setProbe(prob);

        byte[] message = null;
        try{
            message = serializableToBytes(pack);
        }catch(IOException e){
            e.printStackTrace();
        }

        DeprecatedServiceConnection from = new DeprecatedDefaultServiceConnection();

        DeprecatedActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("Store", new TestFactory(), 10);
        DeprecatedServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);
        //local.announce();
        local.discoveryUpdate(from, message);

        DeprecatedServiceConnection dest = new DeprecatedDefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain().into("test");
        assertTrue(RouteSignal.NETWORKDESTINATION == local.getDestination(dest, from, chain));

    }

    private byte[] serializableToBytes(Serializable data) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

}
