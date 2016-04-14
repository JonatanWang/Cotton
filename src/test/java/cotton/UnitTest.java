package test.java.cotton;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import main.java.cotton.mockup.*;

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
    
    private class TestFactory implements ServiceFactory {

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
            this.handler.start();
        }
        
    }
    
    @Test
    public void ThreadPoolTest(){
        ActiveServiceLookup lookup = new DefaultActiveServiceLookup();

        lookup.registerService("test", new TestFactory(), 10);
        ServiceBuffer buffer = new DefaultServiceBuffer();
        ServiceChain to1 = new DummyServiceChain("test");
        to1.addService("test");
        to1.addService("test");
        to1.addService("test");
        DummyBufferStuffer stuffer = new DummyBufferStuffer(buffer,null,"hej",to1);
        stuffer.fillBuffer();
        
        ServiceChain to2 = new DummyServiceChain("test");
        stuffer = new DummyBufferStuffer(buffer,null,"service2",to2);
        stuffer.fillBuffer();
        ServiceHandler handler = new ServiceHandler(lookup,buffer);
        Thread th = new Thread(new Threadrun(handler));
        th.start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        handler.stop();
        System.out.println("text");
        assertNull(buffer.nextPacket());
    }
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
