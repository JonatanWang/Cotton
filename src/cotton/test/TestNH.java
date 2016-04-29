package cotton.test;

import cotton.network.DefaultNetworkHandler;
import cotton.network.DeprecatedDefaultNetworkHandler;
import cotton.network.DummyServiceChain;
import cotton.servicediscovery.DefaultLocalServiceDiscovery;
import cotton.services.DefaultActiveServiceLookup;
import cotton.services.DeprecatedServiceHandler;
import cotton.test.services.MathPow2;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.network.DeprecatedNetworkHandler;
import cotton.servicediscovery.DeprecatedServiceDiscovery;
import cotton.network.DeprecatedServiceRequest;
import cotton.services.DeprecatedActiveServiceLookup;

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

        DeprecatedActiveServiceLookup asl = new DefaultActiveServiceLookup();
        DeprecatedServiceDiscovery sd = new DefaultLocalServiceDiscovery(asl);
        DeprecatedNetworkHandler nh = null;
        try {
            nh = new DeprecatedDefaultNetworkHandler(sd);
        } catch(UnknownHostException e) {}

        DeprecatedServiceHandler dsh = new DeprecatedServiceHandler(asl, nh);
        new Thread(dsh).start();

        asl.registerService("MathPow2", MathPow2.getFactory(), 1);
        DeprecatedServiceRequest sr = null;
        try{
            sr = nh.sendToService(ByteBuffer.allocate(4).putInt(5).array(), new DummyServiceChain().into("MathPow2"));
        }catch(java.io.IOException e){
            e.printStackTrace();
        }

        int result = ByteBuffer.wrap(sr.getData()).getInt();
        assertTrue(25 == result);
    }
}
