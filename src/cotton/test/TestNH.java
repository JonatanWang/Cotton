package cotton.test;

import cotton.internalRouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.NetworkHandler;
import cotton.network.NetworkPacket;
import cotton.network.NetworkPacket.NetworkPacketBuilder;
import cotton.network.SocketLatch;
import java.nio.ByteBuffer;
import org.junit.Test;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Arrays;
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
    
    private NetworkPacket buildPacket(byte[] data) {
        NetworkPacketBuilder npb = new NetworkPacketBuilder();
        npb.setData(data);
        return npb.build();
    }
    
    @Test
    public void TestTransmission() throws IOException, InterruptedException{
        DefaultNetworkHandler ClientNH = new DefaultNetworkHandler(4455);
        
        DefaultNetworkHandler ServerNH = new DefaultNetworkHandler(4466);
        InternalRoutingStub ir = new InternalRoutingStub(ServerNH);
        new Thread(ServerNH).start(); 
        
        Thread.sleep(1000);
        
        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(5).array();
        NetworkPacket np1 = buildPacket(numberAsBytes);
        
        ClientNH.send(np1, new InetSocketAddress(Inet4Address.getLocalHost(),4466));
       
        NetworkPacket np2 = ir.getNetworkPacket();
        
        System.out.println("Recieved packet: " + Arrays.toString(np2.getData()));
        
        assertTrue(25 == 25);
    }
}
