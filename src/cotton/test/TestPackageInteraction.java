package cotton.test;

import cotton.network.DefaultNetworkHandler;
import cotton.network.DummyServiceChain;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import cotton.Cotton;
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.ServiceRequest;
import cotton.network.ServiceChain;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.test.services.MathPowV2;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
 * @author Magnus
 */
public class TestPackageInteraction {

    public TestPackageInteraction() {
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

    private class GlobalDnsStub extends GlobalDiscoveryDNS {

        private SocketAddress[] addressArray = null;

        @Override
        public void setGlobalDiscoveryAddress(SocketAddress[] addresses) {
            this.addressArray = addresses;
        }

        @Override
        public SocketAddress[] getGlobalDiscoveryAddress() {
            return this.addressArray;
        }
    }

    @Test
    public void TestTransmission() throws UnknownHostException {
        Cotton discovery = new Cotton(true, 5555);
        GlobalDnsStub gDns = new GlobalDnsStub();
        
        InetSocketAddress gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), 5555);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);

        discovery.start();

        Cotton ser1 = new Cotton(false, gDns);
        Cotton ser2 = new Cotton(false, gDns);

        ser1.getServiceRegistation().registerService("mathpow2", MathPowV2.getFactory(), 10);
        ser2.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        ser1.start();
        ser2.start();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Cotton cCotton = new Cotton(false, gDns);
        cCotton.start();

        InternalRoutingClient client = cCotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2");

        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        ServiceRequest req = client.sendWithResponse(data, chain);
        if (req != null) {
            byte[] data2 = req.getData();
            int num2 = ByteBuffer.wrap(data2).getInt();
            System.out.println("result: " + num2);
            num = num2;
        } else {
            System.out.println("Failed req: ");
        }

        //Cotton discovery = new Cotton(true,3333);
        //Cotton discovery = new Cotton(true,3333);
        discovery.shutdown();
        ser1.shutdown();
        ser2.shutdown();
        cCotton.shutdown();
        assertTrue(256 == num);
    }
}
