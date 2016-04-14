package cotton.test;

import cotton.services.ActiveServiceLookup;
import cotton.services.CloudContext;
import cotton.services.DefaultActiveServiceLookup;
import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import cotton.test.TestDASL.TestServiceFactory.TestServiceInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class TestDASL {

    public TestDASL() {
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

    public class TestServiceFactory implements ServiceFactory {

        @Override
        public ServiceInstance newServiceInstance() {
            return new TestServiceInstance();
        }

        //TODO Test and change
        public class TestServiceInstance implements ServiceInstance {

            @Override
            public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
                int number = convertInputStream(data);
                
                number *= 2;
                
                return number;
            }

            //TODO Complete
            private int convertInputStream(InputStream data) {
                Integer number = -1;

                ObjectInputStream inStream;
                try {
                    System.out.println("asd");
                    inStream = new ObjectInputStream(data);
                    System.out.println("asd");
                    number = (Integer)inStream.readObject();

                } catch (IOException ex) {
                    Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
                }catch (ClassNotFoundException ex) {
                        Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
                }

                return number;
            }
        }
    }

    @Test
    public void testCapacity() {
        ActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", new TestServiceFactory(), 10);

        assertEquals(10, dasl.getService("Coloring").getMaxCapacity());
    }

    @Test
    public void testWrongCapacity() {
        ActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", new TestServiceFactory(), 10);

        assertNotEquals(9, dasl.getService("Coloring").getMaxCapacity());
    }

    @Test
    public void testHashMapKeys() {
        ActiveServiceLookup dasl = new DefaultActiveServiceLookup();

        String[] services = new String[3];
        services[0] = "Coloring";
        services[1] = "Multiplying";
        services[2] = "Yellow Filtering";

        for(int i = 0; i < services.length; i++)
            dasl.registerService(services[i], new TestServiceFactory(), 10);

        Enumeration<String> hashMapKeys = dasl.getServiceEnumeration();

        // Checks for "Multiplying" in hashMapKeys
        String temp = null;
        while(hashMapKeys.hasMoreElements()) {
            temp = hashMapKeys.nextElement();
            if(temp.equals("Multiplying"))
                break;
        }

        assertEquals("Multiplying", temp);
    }

    @Test
    public void testRemove() {
        ActiveServiceLookup dasl = new DefaultActiveServiceLookup();

        String[] services = new String[3];
        services[0] = "Coloring";
        services[1] = "Multiplying";
        services[2] = "Yellow Filtering";

        for(int i = 0; i < services.length; i++)
            dasl.registerService(services[i], new TestServiceFactory(), 10);

        System.out.println(dasl.removeServiceEntry("Coloring"));

        boolean removeCheck = false;
        if(dasl.getService("Coloring") == null)
            removeCheck = true;

        assertEquals(true, removeCheck);
    }
    
    @Test
    public void testFactory() throws IOException {
        ServiceFactory sf = new TestServiceFactory();
        TestServiceInstance si = (TestServiceInstance)sf.newServiceInstance();
        
        // Pipe connections
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outStream = new PipedOutputStream(in);
        ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
        
        // Number for the service to multiply
        objectOutStream.writeObject(new Integer(2));
        
        objectOutStream.close();
        
        assertEquals(4,si.consumeServiceOrder(null, null, in, null));
        in.close();
    }
}
