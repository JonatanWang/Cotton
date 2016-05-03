package cotton.test;


import cotton.internalRouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DummyServiceChain;
import cotton.network.NetworkHandler;
import cotton.network.NetworkPacket;
import cotton.network.NetworkPacket.NetworkPacketBuilder;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.SocketLatch;
import java.nio.ByteBuffer;
import org.junit.Test;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import static org.junit.Assert.*;
import cotton.network.NetworkHandler;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ActiveServiceLookup;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 * @author Magnus
 */
public class TestNH {
    
    public TestNH() {
    }
    
    public class InternalRoutingStub implements InternalRoutingNetwork {
        private NetworkPacket networkPacket;
        
        public InternalRoutingStub(NetworkHandler nh) throws UnknownHostException {
            nh.setInternalRouting(this);
        }

        @Override
        public void pushNetworkPacket(NetworkPacket networkPacket) {
            this.networkPacket = networkPacket;
            
            System.out.println("bbbbbbbbbbb");
        }
        
        public NetworkPacket getNetworkPacket() {
            return networkPacket;
        }

        @Override
        public void pushKeepAlivePacket(NetworkPacket receivedPacket, SocketLatch latch) {
            System.out.println("wwwwwwwwwwwwww");
            
            int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();
            byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(receivedNumber * 2).array();
            
            networkPacket = rebuildPacket(numberAsBytes, receivedPacket, false);

            latch.setData(networkPacket);
        }
        
        private NetworkPacket rebuildPacket(byte[] data, NetworkPacket np, boolean keepAlive) {
            NetworkPacketBuilder npb = new NetworkPacketBuilder();
            
            npb.setData(data);
            npb.setPath(np.getPath());
            npb.setOrigin(np.getOrigin());
            npb.setKeepAlive(keepAlive);
            npb.setPathType(np.getType());
            
            return npb.build();
        }
    }
    
    private NetworkPacket buildPacket(byte[] data, boolean keepAlive, UUID latch) throws UnknownHostException {
        NetworkPacketBuilder npb = new NetworkPacketBuilder();
        
        npb.setData(data);
        npb.setPath(new DummyServiceChain("sendNumber"));
        Origin origin = new Origin(new InetSocketAddress(Inet4Address.getLocalHost(), 4455), UUID.randomUUID());
        if(latch != null)
            origin.setSocketLatchID(latch);
        npb.setOrigin(origin);
        npb.setKeepAlive(keepAlive);
        npb.setPathType(PathType.SERVICE);
        
        return npb.build();
    }
    
    @Test
    public void TestSend() throws IOException, InterruptedException{
        int numberToSend = 5;
        
        DefaultNetworkHandler clientNH = new DefaultNetworkHandler(4455);
        
        DefaultNetworkHandler serverNH = new DefaultNetworkHandler(4466);
        InternalRoutingStub ir = new InternalRoutingStub(serverNH);
        new Thread(serverNH).start(); 
        
        Thread.sleep(1000);
        
        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(numberToSend).array();
        NetworkPacket sendPacket = buildPacket(numberAsBytes, false, null);
        
        clientNH.send(sendPacket, new InetSocketAddress(Inet4Address.getLocalHost(),4466));
        
        Thread.sleep(1000);
        
        NetworkPacket receivedPacket = ir.getNetworkPacket();
        int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();
        
        assertTrue(5 == receivedNumber);
    }
    
    @Test
    public void TestSendKeepAlive() throws IOException, InterruptedException{
        int numberToSend = 5;
        
        DefaultNetworkHandler clientNH = new DefaultNetworkHandler(5566);        
        DefaultNetworkHandler serverNH = new DefaultNetworkHandler(5577);
        
        InternalRoutingStub serverIR = new InternalRoutingStub(serverNH);
        InternalRoutingStub clientIR = new InternalRoutingStub(clientNH);
        
        new Thread(serverNH).start();
        new Thread(clientNH).start();
        
        Thread.sleep(1000);
        
        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(numberToSend).array();
        NetworkPacket sendPacket = buildPacket(numberAsBytes, true, UUID.randomUUID());
        
        clientNH.sendKeepAlive(sendPacket, new InetSocketAddress(Inet4Address.getLocalHost(),5577));
        
        Thread.sleep(1000);
        
        NetworkPacket receivedPacket = clientIR.getNetworkPacket();
        int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();
        
        System.out.println("Received number: " + receivedNumber);
        
        assertTrue(10 == receivedNumber);
    }
}
