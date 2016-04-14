package test.java.cotton;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;
import main.java.cotton.mockup.ActiveServiceLookup;
import main.java.cotton.mockup.CloudContext;
import main.java.cotton.mockup.DefaultActiveServiceLookup;
import main.java.cotton.mockup.ServiceChain;
import main.java.cotton.mockup.ServiceConnection;
import main.java.cotton.mockup.ServiceFactory;
import main.java.cotton.mockup.ServiceInstance;
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
        
        public class TestServiceInstance implements ServiceInstance {
            
            @Override
            public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
                return null;
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
}
