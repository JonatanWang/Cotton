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
import cotton.test.TestNH.InternalRoutingStub;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import cotton.Cotton;
import cotton.internalRouting.InternalRoutingNetwork;
import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DefaultNetworkHandler;
import cotton.network.ServiceChain;
import cotton.services.CloudContext;
import cotton.network.DummyServiceChain;
import cotton.test.services.MathPowV2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import cotton.network.*;
import cotton.servicediscovery.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import cotton.network.NetworkHandler;
import cotton.services.Service;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceFactory;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceHandler;
import cotton.services.ServiceLookup;
import cotton.services.ServiceMetaData;
import cotton.services.ServicePacket;
import cotton.test.TestNH.InternalRoutingStub;

public class UnitTest {
    public UnitTest() {
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

    @Test
    public void ActiveServiceLookupGetCapacity(){
        ActiveServiceLookup lookup = new ServiceLookup();
        lookup.registerService("hej", null, 10);
        ServiceMetaData service = lookup.getService("hej");
        assertEquals(10,service.getMaxCapacity());
    }

    @Test
    public void ActiveServiceLookupRemove(){
        ActiveServiceLookup lookup = new ServiceLookup();

        lookup.registerService("test", null, 10);

        lookup.removeServiceEntry("test");
        assertNull(lookup.getService("test"));
    }

    private class TestService implements Service{

        @Override
        public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
            String in = "fail";

            ObjectInputStream inStream;
            in = new String(data);

            System.out.println(in);

            return (in+"hej").getBytes();
        }

    }

    public class TestFactory implements ServiceFactory {

        @Override
        public Service newService() {
            return new TestService();
        }

    }

    private class DummyBufferStuffer{
        private ServicePacket servicePacket;
        ServiceBuffer serviceBuffer;
        public DummyBufferStuffer(ServiceBuffer buffer, Origin origin, byte[] data, ServiceChain to){
            this.serviceBuffer = buffer;
            //try{
                /*
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream outStream = new PipedOutputStream(in);
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                objectOutStream.writeObject(data);
                objectOutStream.close();
                */
                this.servicePacket = new ServicePacket(origin, data, to);
                /*}catch(IOException e){
                System.out.println("io exeption");
                e.printStackTrace();
                }*/
        }

        public void fillBuffer(){
            serviceBuffer.add(this.servicePacket);
        }
    }

    private class Threadrun implements Runnable {

        private ServiceHandler handler = null;
        public Threadrun(ServiceHandler handler) {
            this.handler = handler;
        }
        @Override
        public void run() {
            System.out.println("text2");
            //this.handler.start();
            this.handler.run();
        }

    }

    @Test
    public void ThreadPoolTest(){
        
    }

   

//    @Test
//    public void LocalServiceDiscoveryLookup() {
//        System.out.println("LocalServiceDiscoveryLookup");
//
//        ActiveServiceLookup lookup = new ServiceLookup();
//        lookup.registerService("test", new TestFactory(), 10);
//        ServiceDiscovery local = new LocalServiceDiscovery(null);
//        local.setLocalServiceTable(lookup);
//        DestinationMetaData dest = new DestinationMetaData();
//        ServiceChain chain = new DummyServiceChain().into("test");
//        assertTrue(RouteSignal.LOCALDESTINATION == local.getDestination(dest, null, chain));
//    }

//    @Test
//    public void LocalServiceDiscoveryLookupTwoInputs() {
//        System.out.println("LocalServiceDiscoveryLookupTwoInputs, only two imputs");
//
//        ActiveServiceLookup lookup = new ServiceLookup();
//        lookup.registerService("test", new TestFactory(), 10);
//        ServiceDiscovery local = new LocalServiceDiscovery(null);
//        DestinationMetaData dest = new DestinationMetaData();
//        ServiceChain chain = new DummyServiceChain().into("test");
//        assertTrue(RouteSignal.LOCALDESTINATION == local.getDestination(dest, new Origin(), chain));
//    }

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
    
//    @Test
//    public void LocalServiceDiscoveryLookupNullCheck() {
//        System.out.println("LocalServiceDiscoveryLookupNullCheck, checks serviceChain null");
//
//        ActiveServiceLookup lookup = new ServiceLookup();
//        lookup.registerService("test", new TestFactory(), 10);
//        ServiceDiscovery local = new LocalServiceDiscovery(null);
//        InternalRoutingStub ir = new InternalRoutingStub(null);
//        local.setLocalServiceTable(lookup);
//        local.setNetwork((InternalRoutingServiceDiscovery) ir, null);
//        DestinationMetaData dest = new DestinationMetaData();
//        ServiceChain chain = new DummyServiceChain();
//        assertTrue(RouteSignal.NOTFOUND == local.getDestination(dest, null, chain));
//    }

//    @Test
//    public void LocalServiceDiscoveryLookupNullInput() {
//        System.out.println("LocalServiceDiscoveryLookupNullInput, checks destinatin null");
//
//        ActiveServiceLookup lookup = new ServiceLookup();
//        lookup.registerService("test", new TestFactory(), 10);
//        ServiceDiscovery local = new LocalServiceDiscovery(null);
//        ServiceChain chain = new DummyServiceChain();
//        assertTrue(RouteSignal.NOTFOUND == local.getDestination(null,new Origin(), chain));
//    }

//    @Test
//    public void LocalServiceDiscoveryUpdate(){
//        System.out.println("LocalServiceDiscoveryUpdate, testing discoveryUpdate");
//
//        InetSocketAddress name = null;
//        try{
//            name = new InetSocketAddress(InetAddress.getByName(null), 1234);
//        }catch(IOException e){}
//
//        DiscoveryProbe prob = new DiscoveryProbe("test", name);
//
//        DiscoveryPacket pack = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYRESPONSE);
//        //pack.setPacketType(DiscoveryPacketType.DISCOVERYRESPONSE);
//        pack.setProbe(prob);
//
//        byte[] message = null;
//        try{
//            message = serializableToBytes(pack);
//        }catch(IOException e){
//            e.printStackTrace();
//        }
//
//        Origin origin = new Origin();
//
//        ActiveServiceLookup lookup = new ServiceLookup();
//        lookup.registerService("Store", new TestFactory(), 10);
//        ServiceDiscovery local = new LocalServiceDiscovery(null);
//        //local.announce();
//        local.discoveryUpdate(origin, message);
//
//        DestinationMetaData dest = new DestinationMetaData();
//        ServiceChain chain = new DummyServiceChain().into("test");
//        assertTrue(RouteSignal.NETWORKDESTINATION == local.getDestination(dest, origin, chain));
//
//    }

    private byte[] serializableToBytes(Serializable data) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

}
