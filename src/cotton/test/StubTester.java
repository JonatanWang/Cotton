
package cotton.test;

import cotton.Cotton;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.test.services.GlobalDiscoveryAddress;
import cotton.test.stubs.NetStub;
import cotton.test.stubs.NetworkHandlerStub;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author magnus
 */
public class StubTester {
    
    public StubTester() {
    }
    
    @Test
    public void NetStubFakeTest() throws UnknownHostException, IOException {
        System.out.println("Now running: NetStubFakeTest");
        NetStub tubes = new NetStub();
        int discPort = new Random().nextInt(25000) + 4000;
        int queuePort = new Random().nextInt(25000) + 4000;
        Cotton disc = createFakeCotton(tubes,true, discPort);
        disc.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        GlobalDiscoveryAddress dnsStub = new GlobalDiscoveryAddress(discPort);
        String queueName = "mathpow21";
        Cotton reqQueue = createFakeCotton(tubes,false, queuePort, dnsStub);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue(queueName);
        reqQueue.setRequestQueueManager(requestQueueManager);
        reqQueue.start();
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), queuePort);
        DestinationMetaData destination = new DestinationMetaData(addr, PathType.COMMANDCONTROL);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int newAmount = 66;
        Cotton client = createFakeCotton(tubes,false, dnsStub);
        client.start();
        Console console = client.getConsole();
        Command cmd = new Command(StatType.REQUESTQUEUE, "mathPow21", new String[]{queueName, "setMaxCapacity"}, newAmount, CommandType.CHANGE_ACTIVEAMOUNT);
        console.sendCommand(cmd, destination);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Command cmdline = new Command(StatType.REQUESTQUEUE, queueName, new String[]{queueName, "getMaxCapacity"}, newAmount, CommandType.CHANGE_ACTIVEAMOUNT);
        StatisticsData[] query = console.sendQueryCommand(cmdline, destination);
        if (query == null) {
            System.out.println("TestQueueResizeComand: query returnd null");
            assertTrue(false);
        } else if (query.length <= 0) {
            System.out.println("TestQueueResizeComand: query returnd empty result");
            assertTrue(false);
        }
        int[] num = query[0].getNumberArray();
        int maxCapacity = num[0];//requestQueueManager.getMaxCapacity(queueName);
        System.out.println("TestQueueResizeComand:MaxAmount" + maxCapacity + "?=" + newAmount);
        client.shutdown();
        reqQueue.shutdown();
        disc.shutdown();
        assertTrue(maxCapacity == newAmount);
    }
    
        /**
     * SocketAddressEquality checks if socketAddress equals need to be casted to
     * subtype to work
     */
    @Test
    public void SocketAddressEqualityTest() {
        System.out.println("SocketAddressEqualityTest: ...");
        SocketAddress a1 = new InetSocketAddress("127.0.0.1", 3333);
        SocketAddress a2 = new InetSocketAddress("127.0.0.2", 3333);
        SocketAddress a3 = new InetSocketAddress("127.0.0.2", 4662);
        SocketAddress a4 = new InetSocketAddress("127.0.0.1", 1234);
        SocketAddress a5 = new InetSocketAddress("127.0.0.2", 4662);
        SocketAddress a6 = new InetSocketAddress("127.0.0.1", 3333);
        assertFalse(a1.equals(a2));
        assertFalse(a1.equals(a4));
        assertFalse(a3.equals(a4));
        assertTrue(a1.equals(a6));
        assertTrue(a3.equals(a5));
    }
    
    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, int port) throws UnknownHostException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, stub);
    }

    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, int port, GlobalDiscoveryAddress gda) throws UnknownHostException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, gda, stub);
    }
    
    private Cotton createFakeCotton(NetStub tubes, boolean isGlobal, GlobalDiscoveryAddress gda) throws UnknownHostException {
        int port = new Random().nextInt(25000) + 4000;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        NetworkHandlerStub stub = new NetworkHandlerStub(inetSocketAddress);
        stub.setTubes(tubes);
        return new Cotton(isGlobal, gda, stub);
    }
}
