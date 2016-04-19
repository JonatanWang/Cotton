package cotton.test;

import cotton.Cotton;
import cotton.network.ClientNetwork;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import cotton.network.DefaultNetworkHandler;
import cotton.network.NetworkHandler;
import cotton.services.ActiveServiceLookup;
import cotton.services.CloudContext;
import cotton.services.DefaultActiveServiceLookup;
import cotton.services.DummyServiceChain;
import cotton.services.ServiceBuffer;
import cotton.network.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceHandler;
import cotton.services.ServiceInstance;
import cotton.services.ServiceMetaData;
import cotton.services.ServicePacket;
import cotton.test.services.MathPow2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.network.*;
import cotton.servicediscovery.*;


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
        ActiveServiceLookup lookup = new DefaultActiveServiceLookup();
        lookup.registerService("hej", null, 10);
        ServiceMetaData service = lookup.getService("hej");
        assertEquals(10,service.getMaxCapacity());
    }

    @Test
    public void ActiveServiceLookupRemove(){
        ActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", null, 10);

        lookup.removeServiceEntry("test");
        assertNull(lookup.getService("test"));
    }

    private class TestService implements ServiceInstance {

        @Override
        public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
            String in = "fail";

            ObjectInputStream inStream;
            try {
                inStream = new ObjectInputStream(data);
                in = (String)inStream.readObject();

            } catch (IOException ex) {
                Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
            }catch (ClassNotFoundException ex) {
                    Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println(in);

            return in + "hej";
        }

    }

    public class TestFactory implements ServiceFactory {

        @Override
        public ServiceInstance newServiceInstance() {
            return new TestService();
        }

    }

    private class DummyBufferStuffer{
        private ServicePacket servicePacket;
        ServiceBuffer serviceBuffer;
        public DummyBufferStuffer(ServiceBuffer buffer,ServiceConnection from, Serializable data, ServiceChain to){
            this.serviceBuffer = buffer;
            try{
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream outStream = new PipedOutputStream(in);
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                objectOutStream.writeObject(data);
                objectOutStream.close();

                this.servicePacket = new ServicePacket(from,in,to);
            }catch(IOException e){
                System.out.println("io exeption");
                e.printStackTrace();
            }
        }

        public void fillBuffer(){
            serviceBuffer.add(this.servicePacket);
        }


    }

    private class Threadrun implements Runnable {

        private ServiceHandler handler = null;
        public Threadrun(ServiceHandler handler) {
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
        ActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", new TestFactory(), 10);
        NetworkHandler net = new DefaultNetworkHandler();
        ServiceHandler handler = new ServiceHandler(lookup,net);

        ServiceChain to1 = new DummyServiceChain("test");
        to1.addService("test");
        to1.addService("test");
        to1.addService("test");
        net.sendToService("hej", to1, null);

        ServiceChain to2 = new DummyServiceChain("test");
        net.sendToService("service2", to2, null);

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
        Cotton cotton = new Cotton();
        
        ActiveServiceLookup reg = cotton.getServiceRegistation();        
        reg.registerService("MathPow2", MathPow2.getFactory(), 8);
        cotton.start();
        
        ClientNetwork net = cotton.getClientNetwork();
        
        Integer data = new Integer(2);
        ServiceChain chain = new DummyServiceChain()
                .into("MathPow2").into("MathPow2")
                .into("MathPow2").into("MathPow2");
        
        ServiceConnection jobId = net.sendToService(data, chain);
        
        Integer result = (Integer)net.getResults(jobId, null);
        
        System.out.println(result);        
        cotton.shutdown();
        assertTrue(65536 == result.intValue());        
     }

    @Test
    public void LocalServiceDiscoveryLookup() {

        System.out.println("LocalServiceDiscoveryLookup");

        ActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", new TestFactory(), 10);

        LocalServiceDiscovery local = new DefaultLocalServiceDiscovery(lookup);

        ServiceConnection dest = new DefaultServiceConnection();
        ServiceChain chain = new DummyServiceChain().into("test");

        assertTrue(RouteSignal.LOCALDESTINATION == local.getDestination(dest, null, chain));
    }

}
