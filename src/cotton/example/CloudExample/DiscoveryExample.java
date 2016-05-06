/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.CloudExample;

import cotton.Cotton;
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.test.UnitTest;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author o_0
 */
public class DiscoveryExample {
    public static void main(String[] args) throws UnknownHostException {
        Cotton discovery = new Cotton(true, 9546);
        

        discovery.start();
        discovery.shutdown();
    }
}
