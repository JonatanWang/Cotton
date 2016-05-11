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

import cotton.Cotton;
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DummyServiceChain;
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
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.test.services.GlobalDnsStub;
import java.util.Random;

/**
 *
 * @author Magnus
 * @author Tony
 *
 */
public class TestUsageHistory {

    public TestUsageHistory() {
    }

    //@Test
    public void TestQueueUsage() throws UnknownHostException {
        System.out.println("TestQueueUsage");
        Cotton queue = new Cotton(true, null);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue("mathpow21");
        queue.setRequestQueueManager(requestQueueManager);
        queue.start();
        InternalRoutingClient client = queue.getClient();
        
        Console console = queue.getConsole();
        Command command = new Command(StatType.DISCOVERY, "serviceNodes", null, 0, CommandType.STATISTICS_FORSUBSYSTEM);
        command.setQuery(true);
        //console.processCommand();
//        int port = new Random().nextInt(25000) + 5000;
//        Cotton discovery = new Cotton(true,port);
//        
//        GlobalDnsStub gDns = new GlobalDnsStub();
//        InetSocketAddress[] arr = new InetSocketAddress[1];
//        arr[0] = new InetSocketAddress(Inet4Address.getLocalHost(), port);
//        gDns.setGlobalDiscoveryAddress(arr);
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException ex) {
//            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        Cotton queue = new Cotton(false,gDns);
//        RequestQueueManager requestQueueManager = new RequestQueueManager();
//        requestQueueManager.startQueue("mathpow21");
//        queue.setRequestQueueManager(requestQueueManager);
//        queue.start();
//
//        
    }

}
