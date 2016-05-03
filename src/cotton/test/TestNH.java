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
import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan Kåhre
 */
public class TestNH {
    
    public TestNH() {
    }
    
    public class InternalRoutingStub implements InternalRoutingNetwork {

        private NetworkPacket networkPacket = null;
        
        public InternalRoutingStub(NetworkHandler nh) {
            nh.setInternalRouting(this);
        }

        @Override
        public void pushNetworkPacket(NetworkPacket networkPacket) {
            this.networkPacket = networkPacket;
        }
        
        public NetworkPacket getNetworkPacket() {
            return networkPacket;
        }

        @Override
        public void pushKeepAlivePacket(NetworkPacket networkPacket, SocketLatch latch) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    private NetworkPacket buildPacket(byte[] data) throws UnknownHostException {
        NetworkPacketBuilder npb = new NetworkPacketBuilder();
        
        npb.setData(data);
        npb.setPath(new DummyServiceChain("sendNumber"));
        npb.setOrigin(new Origin(new InetSocketAddress(Inet4Address.getLocalHost(), 4455), UUID.randomUUID()));
        npb.setKeepAlive(false);
        npb.setPathType(PathType.SERVICE);
        
        return npb.build();
    }
    
    @Test
    public void TestClientServerTransmission() throws IOException, InterruptedException{
        int numberToSend = 5;
        
        DefaultNetworkHandler ClientNH = new DefaultNetworkHandler(4455);
        
        DefaultNetworkHandler ServerNH = new DefaultNetworkHandler(4466);
        InternalRoutingStub ir = new InternalRoutingStub(ServerNH);
        new Thread(ServerNH).start(); 
        
        Thread.sleep(1000);
        
        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(numberToSend).array();
        NetworkPacket sendPacket = buildPacket(numberAsBytes);
        
        ClientNH.send(sendPacket, new InetSocketAddress(Inet4Address.getLocalHost(),4466));
        
        Thread.sleep(1000);
        
        NetworkPacket receivedPacket = ir.getNetworkPacket();
        
        int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();
        
        assertTrue(5 == receivedNumber);
    }
}
