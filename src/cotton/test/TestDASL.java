package cotton.test;

import cotton.services.CloudContext;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.ServiceChain;
//import cotton.test.TestDASL.TestServiceFactory.TestService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.services.DeprecatedService;
import cotton.services.DeprecatedServiceFactory;
import cotton.services.DeprecatedActiveServiceLookup;
import cotton.network.DeprecatedServiceConnection;

/**
 * A test class for the <code>DefaultActiveServiceLookup</code> class as well as
 * the <code>ServiceFactory</code> interface.
 * 
 * @author Gunnlaugur Juliusson
 */
public class TestDASL {

    /**
     * A test service factory to generate test service instances.
     */
    public class TestServiceFactory implements DeprecatedServiceFactory {

        /**
         * Returns an <code>TestServiceInstance</code> for testing purposes.
         * 
         * @return an TestServiceInstance.
         * @see TestService
         */
        @Override
        public DeprecatedService newService() {
            return new TestService();
        }

        /**
         * A service instance intended for testing the <code>DefaultActiveServiceLookup</code> class.
         */
        public class TestService implements DeprecatedService{

            /**
             * Retrieves the data and converts it to an <code>int</code> and multiplies it by two.
             * The number sent through the <code>InputStream</code> should be defined as an <code>Integer</code>.
             *
             * @param ctx contains the cloud context.
             * @param from describes who sent the original request.
             * @param data the number to be multiplied.
             * @param to describes where the service should redirect the request before returning the number.
             *
             * @return the incoming number multiplied by 2.
             */
            @Override
            public byte[] execute(CloudContext ctx, DeprecatedServiceConnection from, byte[] data, ServiceChain to) {
                int number = ByteBuffer.wrap(data).getInt();
                number *= 2;
                return ByteBuffer.allocate(4).putInt(number).array();
            }
        }
    }

    /**
     * Tests the capacity implementation of the <code>DefaultActiveServiceLookup</code> class.
     * Capacity is set to ten during registration and then compared through the 
     * <code>getMaxCapacity()</code> function.
     */
    @Test
    public void testCapacity() {
        DeprecatedActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", null, 10);

        assertEquals(10, dasl.getService("Coloring").getMaxCapacity());
    }

    /**
     * Tests the capacity implementation of the <code>DefaultActiveServiceLookup</code> class.
     * Capacity is set to ten during registration and then compared to nine through the 
     * <code>getMaxCapacity()</code> function. If the registration is successful the
     * function will respond that the serviceFactory() does not contain a 
     * <code>maxCapacity</code> of nine.
     */
    @Test
    public void testWrongCapacity() {
        DeprecatedActiveServiceLookup dasl = new DefaultActiveServiceLookup();
        dasl.registerService("Coloring", new TestServiceFactory(), 10);
        int maxCapacity = dasl.getService("Coloring").getMaxCapacity();
        assertTrue(9 != maxCapacity);
    }

    /**
     * Tests the hash map keys by comparing existing values with 
     * <code>"Multiplying"</code>. The function registers 3 services and then
     * loops through the entire list checking for <code>"Multiplying"</code>.
     */
    @Test
    public void testHashMapKeys() {
        DeprecatedActiveServiceLookup dasl = new DefaultActiveServiceLookup();

        String[] services = new String[3];
        services[0] = "Coloring";
        services[1] = "Multiplying";
        services[2] = "Yellow Filtering";

        for(int i = 0; i < services.length; i++)
            dasl.registerService(services[i], null, 10);

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

    /**
     * Tests the hash map keys by removing a value and checking whether its 
     * removed or not.
     */
    @Test
    public void testRemove() {
        DeprecatedActiveServiceLookup dasl = new DefaultActiveServiceLookup();

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
    
    /**
     * Tests the <code>ServiceFactory</code> interface by implementing an
     * <code>TestServiceFactory</code>. The test is designed to check whether the
     * design of the interface can be used to implement a new factory as well as
     * services.
     * 
     * @throws IOException thrown if the streams are unsuccessful.
     */
    @Test
    public void testFactory() throws IOException {
        DeprecatedServiceFactory sf = new TestServiceFactory();
        TestServiceFactory.TestService si = (TestServiceFactory.TestService)sf.newService();

        byte[] res = si.execute(null, null, ByteBuffer.allocate(4).putInt(2).array(), null);

        int result = ByteBuffer.wrap(res).getInt();

        assertEquals(4, result);
    }
}
