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

package cotton.example;

import cotton.Cotton;
import cotton.configuration.Configurator;
import cotton.internalrouting.InternalRoutingClient;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DefaultServiceChain;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.requestqueue.RequestQueueManager;
import cotton.services.ActiveServiceLookup;
import cotton.services.CloudContext;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import cotton.test.services.GlobalDiscoveryAddress;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class CottonAPIExample {

    public void globalExample() {
        try {
            Configurator conf = new Configurator("GSDconfig.cfg");
            Cotton cotton = new Cotton(conf);
            cotton.start();
            // do other stuff, while(true)
            cotton.shutdown();

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
            Logger.getLogger(CottonAPIExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void requestQueueExample() throws IOException {
        try {
            Configurator conf = new Configurator("QueueConfig.cfg");
            Cotton cotton = new Cotton(conf);
            RequestQueueManager rqm = cotton.getRequestQueueManager();
            rqm.startQueue("Service1"); // start a queue for Service1
            rqm.startQueue("Service2"); // start a queue for Service2
            cotton.start();
            // do stuff
            cotton.shutdown();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(CottonAPIExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void serviceExample() throws IOException {
        try {
            Configurator conf = new Configurator("QueueConfig.cfg");
            Cotton cotton = new Cotton(conf);
            ActiveServiceLookup serviceRegistation = cotton.getServiceRegistation();
            // registrate Service1, the factory, 
            // and max 5 concurrent threads on this node for this service
            serviceRegistation.registerService("Service1", new Service1Factory(), 5);
            serviceRegistation.registerService("Service2", new Service1Factory(), 7);
            cotton.start();
            // do stuff
            cotton.shutdown();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(CottonAPIExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clientExample1() {
        try {
            Configurator conf = new Configurator("ClientConfig.cfg");
            Cotton cotton = new Cotton(conf);
            cotton.start();
            // do other stuff, while(true)
            InternalRoutingClient client = cotton.getClient();
            byte[] data = "Can be anything".getBytes();
            ServiceChain chain = new DefaultServiceChain().into("Service1").into("Service2");
            client.sendToService(data, chain);
            byte[] data1 = "Can be anything".getBytes();
            ServiceChain chain1 = new DefaultServiceChain().into("Service1").into("Service2");
            ServiceRequest response = client.sendWithResponse(data1, chain1);
            // blocks until it ether timesout or result has been returend
            byte[] result = response.getData();
            System.out.println("This is the result" + new String(result));
            cotton.shutdown();

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
            Logger.getLogger(CottonAPIExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clientExample2() {
        try {
            GlobalDiscoveryAddress gDns = getDiscoveryAddress("127.0.0.1", 5888); // ip, port to global discovery/ cloud
            Cotton cotton = new Cotton(false, gDns);
            cotton.start();
            InternalRoutingClient client = cotton.getClient();
            
            // do stuff
            cotton.shutdown();
        } catch (IOException ex) {
            Logger.getLogger(CottonAPIExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static GlobalDiscoveryAddress getDiscoveryAddress(String dest, int port) throws UnknownHostException {
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();
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

    public static class Service1 implements Service {
        @Override
        public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
            String str = "hi data:" + new String(data);
            return str.getBytes();
        }

        @Override
        public ServiceFactory loadFactory() {
            return new Service1Factory();
        }
    }

    public static class Service1Factory implements ServiceFactory {
        @Override
        public Service newService() {
            return new Service1();
        }
    }
}
