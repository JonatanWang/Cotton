package cotton.test;

import cotton.services.ActiveServiceLookup;
import cotton.services.CloudContext;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.Service;
import cotton.test.TestDASL.TestServiceFactory.TestService;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
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
        public Service newService() {
            return new TestService();
        }

        //TODO Test and change
        public class TestService implements Service{

            @Override
            public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {
                int number = ByteBuffer.wrap(data).getInt();
                number *= 2;
                return ByteBuffer.allocate(4).putInt(number).array();
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
        int maxCapacity = dasl.getService("Coloring").getMaxCapacity();
        assertTrue(9 != maxCapacity);
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
        TestService si = (TestService)sf.newService();

        byte[] res = si.execute(null, null, ByteBuffer.allocate(4).putInt(2).array(), null);

        int result = ByteBuffer.wrap(res).getInt();

        assertEquals(4, result);
    }
}
