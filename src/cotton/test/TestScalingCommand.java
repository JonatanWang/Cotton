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
import cotton.internalrouting.InternalRoutingNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DestinationMetaData;
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
import cotton.services.ServiceHandler;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import java.util.Random;

/**
 *

 * @author Magnus
 */
public class TestScalingCommand {

    public TestScalingCommand() {
    }

    @Test
    public void TestQueueResizeComand() throws UnknownHostException, IOException {
        System.out.println("Now running: TestQueueResizeComand");
        int discPort = new Random().nextInt(25000) + 4000;
        int queuePort =  new Random().nextInt(25000) + 4000;
        Cotton disc = new Cotton(true,discPort);
        disc.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        GlobalDnsStub dnsStub = getDnsStub(null,discPort); 
        String queueName = "mathpow21";
        Cotton reqQueue = new Cotton(false,queuePort,dnsStub);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
        requestQueueManager.startQueue(queueName);
        reqQueue.setRequestQueueManager(requestQueueManager);
        reqQueue.start();
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(),queuePort);
        DestinationMetaData destination = new DestinationMetaData(addr,PathType.COMMANDCONTROL);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int newAmount = 66;
        Cotton client = new Cotton(false,dnsStub);
        client.start();
        Console console = client.getConsole();
        Command cmd = new Command(StatType.REQUESTQUEUE,"mathPow21",new String[]{queueName,"setMaxCapacity"},newAmount,CommandType.CHANGE_ACTIVEAMOUNT);
        console.sendCommand(cmd, destination);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int maxCapacity = requestQueueManager.getMaxCapacity(queueName);
        
        assertTrue(maxCapacity == newAmount);
    }
    
    @Test
    public void TestServiceResizeComand() throws UnknownHostException, IOException {
        System.out.println("Now running: TestQueueResizeComand");
        int discPort = new Random().nextInt(25000) + 4000;
        int servicePort =  new Random().nextInt(25000) + 4000;
        Cotton disc = new Cotton(true,discPort);
        disc.start();
        
        GlobalDnsStub dnsStub = getDnsStub(null,discPort); 
        String serviceName = "mathpow21";
        Cotton serv = new Cotton(false,servicePort,dnsStub);
        serv.getServiceRegistation().registerService(serviceName, MathPowV2.getFactory(), 10);
        serv.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(),servicePort);
        DestinationMetaData destination = new DestinationMetaData(addr,PathType.COMMANDCONTROL);
        
        int newAmount = 66;
        Cotton client = new Cotton(false,dnsStub);
        client.start();
        Console console = client.getConsole();
        Command cmd = new Command(StatType.SERVICEHANDLER,serviceName,new String[]{serviceName,"setMaxCapacity"},newAmount,CommandType.CHANGE_ACTIVEAMOUNT);
        console.sendCommand(cmd, destination);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ServiceHandler sh = (ServiceHandler) console.getProvider(StatType.SERVICEHANDLER);
        Command cmdline = new Command(StatType.SERVICEHANDLER,serviceName,new String[]{serviceName,"getMaxCapacity"},newAmount,CommandType.CHANGE_ACTIVEAMOUNT);
        StatisticsData[] query = console.sendQueryCommand(cmdline, destination);
        if(query == null ) {
            System.out.println("TestQueueResizeComand: query returnd null");
            assertTrue(false);
        }else if(query.length <= 0 ) {
            System.out.println("TestQueueResizeComand: query returnd empty result");
            assertTrue(false);
        }
        int[] num = query[0].getNumberArray();
        int maxCapacity = num[0];//requestQueueManager.getMaxCapacity(queueName);
        System.out.println("TestQueueResizeComand:MaxAmount" +maxCapacity +"?=" +  newAmount);
        assertTrue(maxCapacity == newAmount);
    }
    
    
    
    
    private static GlobalDnsStub getDnsStub(String dest, int port) throws UnknownHostException {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = null;
        if (dest == null) {
            gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
            System.out.println("discAddr:" + Inet4Address.getLocalHost().toString() + " port: " + port);
        } else {
            gdAddr = new InetSocketAddress(dest, port);
        }
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
