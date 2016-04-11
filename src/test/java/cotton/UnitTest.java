package test.java.cotton;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
