package test.java.cotton;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import main.java.cotton.mockup.DefaultActiveServiceLookup;
import main.java.cotton.mockup.ServiceFactory;
import main.java.cotton.mockup.ServiceInstance;
import main.java.cotton.mockup.ServiceMetaData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mr.loser
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

        public TestServiceFactory() {
            
        }
        
        @Override
        public ServiceInstance newServiceInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    @Test
    public void testCapacity() {
        DefaultActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", new TestServiceFactory(), 10);
        
        assertEquals(10, dasl.getService("Coloring").getMaxCapacity());
    }
    
    @Test
    public void testWrongCapacity() {
        DefaultActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", new TestServiceFactory(), 10);
        
        assertNotEquals(9, dasl.getService("Coloring").getMaxCapacity());
    }
    
    @Test
    public void testHashMapKeys() {
        DefaultActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        
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
        DefaultActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        
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
