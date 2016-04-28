package cotton.test;

import cotton.network.DefaultNetworkHandler;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceRequest;
import cotton.servicediscovery.DefaultLocalServiceDiscovery;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ActiveServiceLookup;
import cotton.services.DefaultActiveServiceLookup;
import cotton.services.ServiceHandler;
import cotton.test.services.MathPow2;
import java.net.UnknownHostException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.network.DeprecatedNetworkHandler;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 */
public class TestNH {
    
    public TestNH() {
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
    public void TestTransmission() {
        Integer numberToTest = 5;
        
        ActiveServiceLookup asl = new DefaultActiveServiceLookup();
        ServiceDiscovery sd = new DefaultLocalServiceDiscovery(asl);
        DeprecatedNetworkHandler nh = null;
        try {
            nh = new DefaultNetworkHandler(sd);
        } catch(UnknownHostException e) {}
        
        ServiceHandler dsh = new ServiceHandler(asl, nh);
        new Thread(dsh).start();
        
        asl.registerService("MathPow2", MathPow2.getFactory(), 1);
        ServiceRequest sr = nh.sendToService(numberToTest, new DummyServiceChain().into("MathPow2"));
        
        assertTrue(25 == (Integer)sr.getData());
    }
}
