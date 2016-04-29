package cotton.test;

import cotton.services.*;
import cotton.network.*;
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
public class TestDSB {
    
    public TestDSB() {
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
    public void testAdd() {
        DeprecatedServiceBuffer sb = new DefaultServiceBuffer();
        
        DeprecatedServicePacket sp = new DeprecatedServicePacket(null, null, new DummyServiceChain("Coloring"));
        
        assertEquals(true, sb.add(sp));
    }
    
    @Test
    public void testNextPacket() {
        DeprecatedServiceBuffer sb = new DefaultServiceBuffer();
        
        DeprecatedServicePacket[] sp = new DeprecatedServicePacket[3];
        
        for(int i = 0; i < sp.length; i++)
            sp[i] = new DeprecatedServicePacket(null, null, new DummyServiceChain("Coloring" + Integer.toString(i)));
        
        for(int i = 0; i < sp.length; i++)
            sb.add(sp[i]);
        
        sb.nextPacket();
        sb.nextPacket();
        
        assertEquals("Coloring2", sb.nextPacket().getTo().getNextServiceName());
    }
}
