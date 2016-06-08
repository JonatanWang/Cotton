/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/


package cotton.test;

import cotton.internalrouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DefaultServiceChain;
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
        }

        public NetworkPacket getNetworkPacket() {
            return networkPacket;
        }

        @Override
        public void pushKeepAlivePacket(NetworkPacket receivedPacket, SocketLatch latch) {
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

    private NetworkPacket buildPacket(byte[] data, boolean keepAlive, UUID latch,int port) throws UnknownHostException {
        NetworkPacketBuilder npb = new NetworkPacketBuilder();

        npb.setData(data);
        npb.setPath(new DefaultServiceChain("sendNumber"));
        Origin origin = new Origin(new InetSocketAddress(Inet4Address.getLocalHost(), port), UUID.randomUUID());
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

        int port = 4455;
        NetworkHandler clientNH = new DefaultNetworkHandler(port);

        NetworkHandler serverNH = new DefaultNetworkHandler(4466);
        InternalRoutingStub ir = new InternalRoutingStub(serverNH);
        new Thread(serverNH).start(); 

        Thread.sleep(1000);

        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(numberToSend).array();
        NetworkPacket sendPacket = buildPacket(numberAsBytes, false, null,port);

        clientNH.send(sendPacket, new InetSocketAddress(Inet4Address.getLocalHost(),4466));

        Thread.sleep(1000);

        NetworkPacket receivedPacket = ir.getNetworkPacket();
        int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();

        assertTrue(5 == receivedNumber);
    }

    @Test
    public void TestSendKeepAlive() throws IOException, InterruptedException{
        int numberToSend = 5;

        int port = 5566;
        NetworkHandler clientNH = new DefaultNetworkHandler(port);
        NetworkHandler serverNH = new DefaultNetworkHandler(5577);

        InternalRoutingStub serverIR = new InternalRoutingStub(serverNH);
        InternalRoutingStub clientIR = new InternalRoutingStub(clientNH);

        new Thread(serverNH).start();
        new Thread(clientNH).start();

        Thread.sleep(1000);

        byte[] numberAsBytes = ByteBuffer.allocate(4).putInt(numberToSend).array();
        NetworkPacket sendPacket = buildPacket(numberAsBytes, true, UUID.randomUUID(),port);

        clientNH.sendKeepAlive(sendPacket, new InetSocketAddress(Inet4Address.getLocalHost(),5577));

        Thread.sleep(1000);

        NetworkPacket receivedPacket = clientIR.getNetworkPacket();

        int receivedNumber = ByteBuffer.wrap(receivedPacket.getData()).getInt();

        System.out.println("Received number: " + receivedNumber);

        assertTrue(10 == receivedNumber);
    }
}
