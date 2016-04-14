package cotton.test;

import main.java.cotton.mockup.DefaultServiceBuffer;
import main.java.cotton.mockup.DummyServiceChain;
import main.java.cotton.mockup.ServiceBuffer;
import main.java.cotton.mockup.ServicePacket;
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
        ServiceBuffer sb = new DefaultServiceBuffer();
        
        ServicePacket sp = new ServicePacket(null, null, new DummyServiceChain("Coloring"));
        
        assertEquals(true, sb.add(sp));
    }
    
    @Test
    public void testNextPacket() {
        ServiceBuffer sb = new DefaultServiceBuffer();
        
        ServicePacket[] sp = new ServicePacket[3];
        
        for(int i = 0; i < sp.length; i++)
            sp[i] = new ServicePacket(null, null, new DummyServiceChain("Coloring" + Integer.toString(i)));
        
        for(int i = 0; i < sp.length; i++)
            sb.add(sp[i]);
        
        sb.nextPacket();
        sb.nextPacket();
        
        assertEquals("Coloring2", sb.nextPacket().getTo().getNextServiceName());
    }
}
