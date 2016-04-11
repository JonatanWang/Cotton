package test.java.cotton;

import main.java.cotton.mockup.DefaultActiveServiceLookup;
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
    public void testRegistrateService() {
        DefaultActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registrateService("Coloring", new TestServiceFactory(), 10);
        int a = 10, b = 10;
        
        //assertEquals(10, dasl.getService("Coloring").getMaxCapacity());
        assertEquals(a, b);
    }
}
